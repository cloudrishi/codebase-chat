import os
import pytest
from unittest.mock import patch
from app.main import is_safe_path


class TestIsSafePath:

    def test_path_within_allowed_base_returns_true(self, tmp_path):
        safe_dir = str(tmp_path)
        with patch("app.main.ALLOWED_BASE_PATHS", [str(tmp_path)]):
            assert is_safe_path(safe_dir) is True

    def test_path_nested_within_allowed_base_returns_true(self, tmp_path):
        nested = tmp_path / "subdir" / "project"
        nested.mkdir(parents=True)
        with patch("app.main.ALLOWED_BASE_PATHS", [str(tmp_path)]):
            assert is_safe_path(str(nested)) is True

    def test_path_outside_allowed_base_returns_false(self, tmp_path):
        outside = tmp_path / ".." / "other"
        with patch("app.main.ALLOWED_BASE_PATHS", ["/allowed/projects"]):
            assert is_safe_path(str(outside)) is False

    def test_path_traversal_attempt_returns_false(self):
        traversal = "/allowed/projects/../../etc/passwd"
        with patch("app.main.ALLOWED_BASE_PATHS", ["/allowed/projects"]):
            assert is_safe_path(traversal) is False

    def test_empty_string_returns_false(self):
        with patch("app.main.ALLOWED_BASE_PATHS", ["/allowed/projects"]):
            assert is_safe_path("") is False

    def test_root_path_returns_false(self):
        with patch("app.main.ALLOWED_BASE_PATHS", ["/allowed/projects"]):
            assert is_safe_path("/") is False

    def test_multiple_allowed_bases_matches_second(self, tmp_path):
        first_base = "/not/this/one"
        second_base = str(tmp_path)
        with patch("app.main.ALLOWED_BASE_PATHS", [first_base, second_base]):
            assert is_safe_path(str(tmp_path)) is True

    def test_allowed_base_prefix_collision_returns_false(self, tmp_path):
        # /allowed/projects-evil should NOT match /allowed/projects
        base = str(tmp_path / "projects")
        evil = str(tmp_path / "projects-evil")
        os.makedirs(base, exist_ok=True)
        os.makedirs(evil, exist_ok=True)
        with patch("app.main.ALLOWED_BASE_PATHS", [base]):
            assert is_safe_path(evil) is False
