#!/usr/bin/env python3
"""Contrato estático del módulo del dragón. No reemplaza pruebas en WebView/Android."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]
HTML = (ROOT / "index.html").read_text(encoding="utf-8")

class DragonContract(unittest.TestCase):
    def test_six_compatible_evolution_stages(self):
        section = HTML.split("var nsDragonStages=[", 1)[1].split("];", 1)[0]
        self.assertEqual(section.count("min:"), 6)
        for boundary in ("min:0", "min:80", "min:200", "min:420", "min:750", "min:1200"):
            self.assertIn(boundary, section)

    def test_previous_pet_schema_survives(self):
        for key in ("p.xp=", "p.food=", "p.feeds=", "p.name=", "p.accessory=",
                    "p.room=", "p.furniture=", "p.missionClaims="):
            self.assertIn(key, HTML)
        self.assertIn("p.energy=Math.min(100", HTML)
        self.assertIn("p.plays=Math.max(0", HTML)

    def test_animations_and_accessibility(self):
        for action in ("walk", "sleep", "eat", "play", "celebrate"):
            self.assertIn('data-dragon-action="'+action+'"', HTML)
        for feature in ("ns-dragon-tail", "ns-dragon-eye", "ns-dragon-wing",
                        "ns-dragon-float", "prefers-reduced-motion:reduce"):
            self.assertIn(feature, HTML)
        self.assertIn('id="ns-dragon-play-controls"', HTML)

    def test_no_reward_for_repeated_play(self):
        play = HTML.split("function nsDragonPlay(){", 1)[1].split("function nsDragonRest(){", 1)[0]
        self.assertIn("p.energy-=15", play)
        self.assertNotRegex(play, r"p\.(?:xp|food)\s*\+=")
        self.assertIn("save()", play)

    def test_existing_task_and_focus_reward_hooks(self):
        self.assertIn("model.pet.xp+=10", HTML)
        self.assertIn("p.xp+=Math.max(5", HTML)
        self.assertIn("nsDragonClaim(m.id)", HTML)

    def test_exactly_one_initializer_and_script(self):
        self.assertEqual(HTML.count("nsDragonInitActions();"), 1)
        self.assertEqual(len(re.findall(r"<script(?:\s|>)", HTML)), 1)
        self.assertEqual(HTML.count("</script>"), 1)

if __name__ == "__main__":
    unittest.main()
