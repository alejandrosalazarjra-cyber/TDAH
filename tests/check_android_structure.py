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
for name in ("AccountStore.java", "AlarmRingActivity.java", "TodayWidget.java", "SyncService.java", "SessionVault.java", "AlarmChallengeStore.java"):
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
        if ".FocusNotificationListener" in services:
            issues.append("Restricted notification listener must not be enabled by default")
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
    for method in ("openMinimalHome", "requestDefaultHome", "scheduleAlarmRepeat", "alarmPermissions", "restoreSession"):
        count = len(re.findall(r"\b(?:void|boolean|String)\s+" + re.escape(method) + r"\s*\(", main))
        if count != 1:
            issues.append(f"MainActivity {method} declared {count} times, expected 1")
    for required in ("SessionVault.restore", "SessionVault.save", "SessionVault.clear"):
        if required not in main:
            issues.append(f"Missing protected session integration: {required}")
    vault = require_file(JAVA / "SessionVault.java")
    if "AndroidKeyStore" not in vault or "AES/GCM/NoPadding" not in vault:
        issues.append("Persistent session must be protected by Android Keystore AES-GCM")
    account = require_file(JAVA / "AccountStore.java")
    if "tokenHash" not in account or "ns_local_session_index" not in account:
        issues.append("Local sessions must support restoration without plain-text credentials")
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
    if "Settings.ACTION_APP_NOTIFICATION_SETTINGS" not in launcher:
        issues.append("Notification controls must use Android per-app settings instead of privileged listening")
    if "Intent.ACTION_DIAL" not in launcher:
        issues.append("Launcher must retain access to phone dialer")
    if "Settings.ACTION_HOME_SETTINGS" not in launcher:
        issues.append("Launcher must allow reverting home app")

if audio and ("startForeground" not in audio or "USAGE_ALARM" not in audio):
    issues.append("AlarmAudioService must start foreground audio using alarm stream")
if alarm and "AlarmAudioService.start" not in alarm:
    issues.append("AlarmReceiver must start sound automatically on alarm firing")
if alarm:
    if "AlarmChallengeStore.get" not in alarm:
        issues.append("Alarm notification must respect challenge mode")
    if "AlarmChallengeStore.remove" not in alarm:
        issues.append("Canceling an alarm must clear its challenge setting")
ring = require_file(JAVA / "AlarmRingActivity.java")
for feature in ('"math"', '"taps"', "finishAlarm()", "Detener sin desafío"):
    if feature not in ring:
        issues.append(f"Missing safe alarm challenge behavior: {feature}")
main = require_file(JAVA / "MainActivity.java")
if "AlarmClock.ACTION_SHOW_ALARMS" not in main or 'com.google.android.deskclock' not in main:
    issues.append("Google Clock compatibility fallback missing")
if alarm and "setFullScreenIntent" not in alarm:
    issues.append("AlarmReceiver missing full-screen alarm notification")

if issues:
    for issue in issues:
        print(f"::error::{issue}")
    sys.exit(1)
print("Android structure OK; full Java compile and device testing are separate checks.")
