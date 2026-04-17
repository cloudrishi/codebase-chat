from platform import node

import javalang
from dataclasses import dataclass
from typing import Optional

@dataclass
class CodeChunk:
    file_path: str
    class_name: Optional[str]
    method_name: Optional[str]
    chunk_type: str  # e.g., 'class', 'method', 'field', 'constructor'
    content: str

def _find_closing_brace(lines: list[str], start: int) -> int:
    """
    Finds the true end of a Java method or class by counting
    opening and closing braces from the start line.
    Returns the line index of the closing brace, or a
    safe fallback if the structure cannot be determined.
    """
    depth = 0
    found_first_brace = False

    for i, line in enumerate(lines[start:], start):
        opening = line.count('{')
        closing = line.count('}')

        if opening > 0:
            found_first_brace = True

        depth += opening
        depth -= closing

        if found_first_brace and depth == 0:
            return i + 1  # +1 to include the closing brace line

    # Fallback if brace matching fails
    return min(start + 50, len(lines))

def parse_java_file(file_path: str) -> list[CodeChunk]:
    """
    Reads a Java file and splits it into meaningful chunks based by
    class and method boundaries using AST parsing.
    """

    chunks = []
    with open(file_path, 'r', encoding='utf-8') as f:
        source = f.read()
    
    try:
       tree = javalang.parse.parse(source)
    except javalang.parser.JavaSyntaxError as e:
        print(f"Skiping {file_path} - syntax error: {e}")
        return chunks
    
    lines = source.splitlines()
    for _, node in tree.filter(javalang.tree.ClassDeclaration):
        class_chunk = _extract_class_chunk(node, lines, file_path)
        if class_chunk:
            chunks.append(class_chunk)
        
        for method in node.methods:
            method_chunk = _extract_method_chunk(method, node.name,lines, file_path)
            if method_chunk:
                chunks.append(method_chunk)

    for _, node in tree.filter(javalang.tree.InterfaceDeclaration):
        chunks.append(CodeChunk(
            file_path=file_path,
            class_name=node.name,
            method_name=None,
            chunk_type='interface',
            content=f"interface {node.name}" + "{ ... }"))

    return chunks

def _extract_class_chunk(node, lines, file_path) -> Optional[CodeChunk]:
    if not node.position:
        return None
    start = node.position.line - 1
    end = _find_closing_brace(lines, start)

    # Cap at 100 lines to avoid embedding truncation
    end = min(end, start + 100)

    raw_content = "\n".join(lines[start:end])
    content = f"Class: {node.name}\n\n{raw_content}"
    return CodeChunk(
        file_path=file_path,
        class_name=node.name,
        method_name=None,
        chunk_type='class',
        content=content
    )

def _extract_method_chunk(node, class_name, lines, file_path) -> Optional[CodeChunk]:
    if not node.position:
        return None
    start = node.position.line - 1
    end = _find_closing_brace(lines, start)
    # Cap at 80 lines to stay within embedding token limit
    end = min(end, start + 80)
    raw_content = "\n".join(lines[start:end])
    content = f"Class: {class_name}\nMethod: {node.name}\n\n{raw_content}"
    return CodeChunk(
        file_path=file_path,
        class_name=class_name,
        method_name=node.name,
        chunk_type='method',
        content=content
    )