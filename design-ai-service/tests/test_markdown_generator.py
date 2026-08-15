import json
import unittest

from app.generators.markdown_generator import MarkdownGenerator


class MarkdownGeneratorTest(unittest.TestCase):
    def test_generates_agent_contract_from_aggregated_data(self) -> None:
        style = {
            "metadata": {"pageCount": 3},
            "colors": [{"value": "#ffffff", "role": "background", "usageCount": 20, "pageCount": 3, "pageCoverage": 1.0, "prominenceScore": 0.9, "contexts": ["body"]}],
            "typography": [{"fontFamily": "Inter", "fontSize": "16px", "fontWeight": "400", "lineHeight": "24px", "letterSpacing": "0px", "usageCount": 15, "pageCount": 3, "pageCoverage": 1.0}],
            "spacing": [{"value": "8px", "pixels": 8, "usageCount": 10, "pageCount": 3, "pageCoverage": 1.0}],
            "radii": [], "shadows": [], "cssVariables": [],
        }
        markdown = MarkdownGenerator().generate("analysis-1", style, ["Keep the scale compact."], 0.8)
        self.assertIn("# DESIGN.md", markdown)
        self.assertIn('"canvas": "#ffffff"', markdown)
        self.assertIn('"space-8": "8px"', markdown)
        json_block = markdown.split("```json\n", 1)[1].split("\n```", 1)[0]
        manifest = json.loads(json_block)
        self.assertEqual(3, manifest["pagesAnalyzed"])
        self.assertEqual("#ffffff", manifest["tokens"]["color"]["canvas"])


if __name__ == "__main__":
    unittest.main()
