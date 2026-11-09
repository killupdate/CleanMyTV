#!/usr/bin/env python3
"""Offline source-policy checks only. Does not build, run project code, or contact a TV/network."""
from pathlib import Path
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
EXCLUDED = {'HomeKeyService.java', 'InputKeyService.java', 'GuardKeep.java',
            'GuardConfig.java', 'GuardRevival.java', 'ServiceRepair.java',
            'ThemeProvider.java', 'ProbeActivity.java'}
patterns = {
    'network_clients': r'\b(?:java\.net|javax\.net|okhttp3?|retrofit2?|HttpURLConnection|Socket|WebView|ConnectivityManager|WifiManager)\b',
    'process_execution': r'\b(?:ProcessBuilder|Runtime\s*\.\s*getRuntime|System\s*\.\s*load(?:Library)?)\b',
    'dynamic_code': r'\b(?:DexClassLoader|PathClassLoader|InMemoryDexClassLoader|createPackageContext|Class\s*\.\s*forName)\b',
    'privileged_writes': r'Settings\s*\.\s*(?:Secure|Global|System)\s*\.\s*put\w+',
}

def run():
    manifest = ET.parse(ROOT / 'launcher/app/src/main/AndroidManifest.xml').getroot()
    permissions = [e.get(ANDROID + 'name') for e in manifest if e.tag.startswith('uses-permission')]
    errors = []
    if permissions: errors.append('Source manifest must request no permissions')
    build = (ROOT / 'launcher/app/build.gradle').read_text()
    for name in EXCLUDED:
        if "'**/" + name + "'" not in build: errors.append('Missing source exclusion: ' + name)
    if not re.search(r'\bminSdk\s+24\b', build): errors.append('minSdk must be 24')
    if "applicationId 'com.killupdate.cleanmytv'" not in build: errors.append('Unexpected application ID')
    if 'signingConfig signingConfigs.debug' in build: errors.append('Release must not use debug signing')
    if 'coreLibraryDesugaringEnabled true' not in build: errors.append('Missing java.time compatibility')
    components = []
    app = manifest.find('application')
    for e in app:
        if e.tag not in ('activity', 'service', 'receiver', 'provider'): continue
        name = e.get(ANDROID + 'name'); components.append({'type':e.tag,'name':name,'exported':e.get(ANDROID+'exported')})
        if e.tag in ('receiver', 'provider'): errors.append('Unexpected receiver/provider: ' + str(name))
        if e.tag == 'service' and name != '.ClockDream': errors.append('Unexpected service: ' + str(name))
        if e.get(ANDROID+'exported') == 'true' and name not in ('.MainActivity', '.ClockDream'):
            errors.append('Unexpected exported component: ' + str(name))
        if name == '.ClockDream' and e.get(ANDROID+'permission') != 'android.permission.BIND_DREAM_SERVICE':
            errors.append('Screensaver must require system binding permission')
    sources = sorted((ROOT / 'launcher/app/src/main/java').rglob('*.java'))
    findings = []
    hashes = {}
    for p in sources:
        data = p.read_bytes(); hashes[str(p.relative_to(ROOT))] = hashlib.sha256(data).hexdigest()
        if p.name in EXCLUDED: continue
        # Search actual text, including comments: false positives must be reviewed, never silently waived.
        for kind, pattern in patterns.items():
            for match in re.finditer(pattern, data.decode()):
                findings.append({'kind':kind,'path':str(p.relative_to(ROOT)),
                                 'line':data[:match.start()].count(b'\n')+1,'match':match.group()})
    if findings: errors.append('Review source findings')
    if list((ROOT/'launcher').rglob('*.so')): errors.append('Unexpected native libraries in launcher tree')
    if (ROOT/'.github/workflows').exists(): errors.append('Active Actions directory present')
    wrappers = sorted((ROOT/'launcher').rglob('gradle-wrapper.jar'))
    expected = '498495120a03b9a6ab5d155f5de3c8f0d986a449153702fb80fc80e134484f17'
    for p in wrappers:
        if hashlib.sha256(p.read_bytes()).hexdigest() != expected: errors.append('Wrapper hash mismatch')
    xml_count = 0
    for p in (ROOT/'launcher/app/src/main').rglob('*.xml'): ET.parse(p); xml_count += 1
    for p in (ROOT/'launcher').rglob('settings.gradle'):
        if 'gradle.startParameter.offline = true' not in p.read_text(): errors.append('Offline build policy missing')
    result = {'scope':'source policy only; NOT merged Manifest, DEX, APK, full API lint or malware certification',
              'source_java_files':len(sources), 'included_java_files':len(sources)-len(EXCLUDED),
              'resource_and_manifest_xml_files':xml_count, 'excluded_java':sorted(EXCLUDED),
              'source_permissions':permissions, 'components':components, 'findings':findings,
              'errors':errors, 'java_sha256':hashes, 'apk_status':'NOT_BUILT',
              'build_status':'BLOCKED: no JDK/SDK/Gradle; owner declined toolchain download'}
    print(json.dumps(result,ensure_ascii=False,indent=2))
    return bool(errors)
if __name__ == '__main__': sys.exit(run())
