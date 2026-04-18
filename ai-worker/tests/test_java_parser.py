import pytest
from app.parser.java_parser import _find_closing_brace


class TestFindClosingBrace:

    def test_simple_method_returns_correct_end(self):
        lines = [
            "public void hello() {",
            "    System.out.println(\"hello\");",
            "}",
        ]
        result = _find_closing_brace(lines, 0)
        assert result == 3  # includes the closing brace line

    def test_nested_braces_resolves_to_outer_close(self):
        lines = [
            "public void check(int x) {",
            "    if (x > 0) {",
            "        return;",
            "    }",
            "}",
        ]
        result = _find_closing_brace(lines, 0)
        assert result == 5

    def test_deeply_nested_braces(self):
        lines = [
            "public void process() {",
            "    for (int i = 0; i < 10; i++) {",
            "        if (i % 2 == 0) {",
            "            doSomething();",
            "        }",
            "    }",
            "}",
        ]
        result = _find_closing_brace(lines, 0)
        assert result == 7

    def test_start_in_middle_of_lines(self):
        lines = [
            "// some preamble",
            "public void middle() {",
            "    doWork();",
            "}",
            "// trailing content",
        ]
        result = _find_closing_brace(lines, 1)
        assert result == 4

    def test_unclosed_brace_returns_fallback(self):
        lines = [
            "public void broken() {",
            "    doSomething();",
            # no closing brace
        ]
        result = _find_closing_brace(lines, 0)
        # fallback: start + 50 capped at len(lines)
        assert result == len(lines)

    def test_empty_lines_returns_fallback(self):
        result = _find_closing_brace([], 0)
        assert result == 0

    def test_brace_in_string_literal_does_not_confuse_counter(self):
        # Brace counting is naive — this documents known behaviour
        lines = [
            'public void withString() {',
            '    String s = "hello {world}";',
            '}',
        ]
        # The braces inside the string literal will confuse the counter —
        # this test documents the current behaviour, not ideal behaviour
        result = _find_closing_brace(lines, 0)
        # depth: line0 +1=1, line1 +1-1=1, line2 -1=0 → closes at line 2
        assert result == 3

    def test_single_line_method(self):
        lines = [
            "public int getValue() { return value; }",
        ]
        result = _find_closing_brace(lines, 0)
        assert result == 1
