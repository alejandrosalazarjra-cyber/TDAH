#!/usr/bin/env python3
"""Static structure checks for the Android APK. Gradle still provides the compile verdict."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "android/app/src/main"
JAVA = ANDROID / "java/com/neurosync/familia"
NS = "{http://schemas.android.com/apk/res/android}"
issues = []

def require_file(path):
    if not path.is_file() or path.stat().st_size == 0:
        issues.append(f"Missing or empty: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")

manifest_text = require_file(ANDROID / "AndroidManifest.xml")
launcher = require_file(JAVA / "FocusLauncherActivity.java")
main = require_file(JAVA / "MainActivity.java")
alarm = require_file(JAVA / "AlarmReceiver.java")
audio = require_file(JAVA / "AlarmAudioService.java")
filter_service = require_file(JAVA / "FocusNotificationListener.java")
for name in ("AccountStore.java", "AlarmRingActivity.java", "TodayWidget.java", "SyncService.java"):
    require_file(JAVA / name)
for resource in ("res/layout/widget_today.xml", "res/xml/today_widget_info.xml",
                 "res/drawable/ic_neurosync.xml"):
    require_file(ANDROID / resource)

if manifest_text:
    try:
        tree = ET.fromstring(manifest_text)
        homes = []
        for activity in tree.findall(".//activity"):
            for rule in activity.findall("intent-filter"):
                categories = {c.get(NS+"name") for c in rule.findall("category")}
                if "android.intent.category.HOME" in categories:
                    homes.append(activity.get(NS+"name"))
        if homes != [".FocusLauncherActivity"]:
            issues.append(f"Expected one HOME activity FocusLauncherActivity, found {homes}")
        services = {s.get(NS+"name") for s in tree.findall(".//service")}
        if ".FocusNotificationListener" not in services:
            issues.append("Notification listener not declared")
        if ".AlarmAudioService" not in services:
            issues.append("Alarm sound foreground service not declared")
        if ".SyncService" not in services:
            issues.append("Family sync service not declared")
        if len(tree.findall("queries")) != 1:
            issues.append("Must have one app visibility <queries> declaration")
        permissions = [x.get(NS+"name") for x in tree.findall("uses-permission")]
        if len(set(permissions)) != len(permissions):
            issues.append("Duplicate Android permissions")
    except ET.ParseError as error:
        issues.append(f"Invalid manifest XML: {error}")

if main:
    for method in ("openMinimalHome", "requestDefaultHome", "scheduleAlarmRepeat", "alarmPermissions"):
        count = len(re.findall(r"\b(?:void|boolean|String)\s+" + re.escape(method) + r"\s*\(", main))
        if count != 1:
            issues.append(f"MainActivity {method} declared {count} times, expected 1")
    if "HomeActivity.class" in main:
        issues.append("Stale launcher HomeActivity referenced from bridge")

if launcher:
    for method in ("chooseAppLimit", "showUsageReport", "chooseFocusHours", "chooseDefaultHome",
                   "openNeuroSync", "delayLaunch", "chooseFocusSession", "focusRemaining"):
        if not re.search(r"\b"+re.escape(method)+r"\s*\(", launcher):
            issues.append(f"Missing launcher feature {method}")
    for feature in ("focus_session_until", "daily_budget", "app_limit_", "mindful_delay",
                    "Settings.ACTION_USAGE_ACCESS_SETTINGS"):
        if feature not in launcher:
            issues.append(f"Missing focus feature: {feature}")
    if "Intent.ACTION_DIAL" not in launcher:
        issues.append("Launcher must retain access to phone dialer")
    if "Settings.ACTION_HOME_SETTINGS" not in launcher:
        issues.append("Launcher must allow reverting home app")

if filter_service and ("CATEGORY_ALARM" not in filter_service or "CATEGORY_CALL" not in filter_service):
    issues.append("Notification filter needs call and alarm exclusions")
if audio and ("startForeground" not in audio or "USAGE_ALARM" not in audio):
    issues.append("AlarmAudioService must start foreground audio using alarm stream")
if alarm and "AlarmAudioService.start" not in alarm:
    issues.append("AlarmReceiver must start sound automatically on alarm firing")
if alarm and "setFullScreenIntent" not in alarm:
    issues.append("AlarmReceiver missing full-screen alarm notification")

if issues:
    for issue in issues:
        print(f"::error::{issue}")
    sys.exit(1)
print("Android structure OK; full Java compile and device testing are separate checks.")
