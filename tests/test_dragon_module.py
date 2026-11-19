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

    def test_celebration_is_gated_by_real_task_completion(self):
        self.assertIn("if(awarded){nsDragonMessage(", HTML)
        self.assertIn("nsDragonAnimate('celebrate')", HTML)
        self.assertIn("nsDragonCheckEvolution(idx)", HTML)
        self.assertIn("nsDragonPending", HTML)

    def test_higgsfield_sprites_all_six_exist(self):
        for stage in range(1, 7):
            asset = ROOT / "assets" / "dragon" / f"etapa-{stage}.webp"
            self.assertTrue(asset.is_file(), f"Falta {asset}")
            self.assertGreater(asset.stat().st_size, 1000)
            self.assertEqual(asset.read_bytes()[:4], b"RIFF")
            self.assertEqual(asset.read_bytes()[8:12], b"WEBP")

    def test_evolution_sprite_is_local_and_svg_fallback_remains(self):
        self.assertIn("image.src='assets/dragon/etapa-'+(idx+1)+'.webp'", HTML)
        self.assertIn("figure.innerHTML=nsDragonSvg(idx,p.accessory)", HTML)
        self.assertIn("figure.classList.add('has-sprite')", HTML)
        self.assertIn("image.onerror=function()", HTML)

    def test_evolution_atlases_all_six(self):
        for stage in range(1, 7):
            asset = ROOT / "assets" / "dragon" / "atlases" / f"etapa-{stage}.webp"
            self.assertTrue(asset.is_file(), f"Falta {asset}")
            self.assertGreater(asset.stat().st_size, 10000)
            self.assertEqual(asset.read_bytes()[0:4], b"RIFF")
            self.assertEqual(asset.read_bytes()[8:12], b"WEBP")
        for pose in ("idle", "blink", "walk-a", "walk-b", "flap", "sleep", "eat", "celebrate"):
            self.assertIn("'" + pose + "'", HTML)
        self.assertIn("function nsDragonPoseSet(state)", HTML)
        self.assertIn("nsDragonPoseFrameTimer=setInterval", HTML)
        self.assertIn("nsDragonAtlasFrame(figure,1)", HTML)
        self.assertIn("nsDragonStopPoseTimers()", HTML)
        self.assertIn("figure.classList.add('has-atlas')", HTML)

    def test_android_packages_and_serves_offline_sprites(self):
        workflow = (ROOT / ".github" / "workflows" / "android-apk.yml").read_text(encoding="utf-8")
        java = (ROOT / "android" / "app" / "src" / "main" / "java"
                / "com" / "neurosync" / "familia" / "MainActivity.java").read_text(encoding="utf-8")
        self.assertIn("cp assets/dragon/etapa-{1..6}.webp", workflow)
        self.assertIn('path.matches("/assets/dragon/', java)
        self.assertIn('cp assets/dragon/atlases/etapa-{1..6}.webp', workflow)
        self.assertIn('atlases/etapa-[1-6]', java)
        self.assertIn('getAssets().open(file)', java)

    def test_exactly_one_initializer_and_script(self):
        self.assertEqual(HTML.count("nsDragonInitActions();"), 1)
        # Only one inline initializer; standalone modules are loaded externally.
        inline_scripts = [tag for tag in re.findall(r'<script(?:\\s[^>]*)?>', HTML)
                          if not re.search(r'\\bsrc\\s*=', tag)]
        self.assertEqual(len(inline_scripts), 1)
        total_scripts = len(re.findall(r"<script(?:\\s|>)", HTML))
        self.assertEqual(HTML.count("</script>"), total_scripts)
        for module in ("wellbeing_studio", "finance_studio", "pomodoro_studio",
                       "departure_studio", "alarm_studio", "task_studio"):
            self.assertEqual(HTML.count(f'<script src="./modules/{module}.js">'), 1)

if __name__ == "__main__":
    unittest.main()
