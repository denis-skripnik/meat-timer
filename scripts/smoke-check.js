const fs = require('fs');
const vm = require('vm');

const html = fs.readFileSync('index.html', 'utf8');
const sw = fs.readFileSync('sw.js', 'utf8');

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function extractScript(source) {
  const match = source.match(/<script>([\s\S]*?)<\/script>/);
  assert(match, 'Inline app script was not found.');
  return match[1];
}

function countMatches(source, regex) {
  const matches = source.match(regex);
  return matches ? matches.length : 0;
}

function getFunctionBody(source, functionName) {
  const marker = `function ${functionName}`;
  const start = source.indexOf(marker);
  assert(start >= 0, `${functionName}() was not found.`);
  const open = source.indexOf('{', start);
  let depth = 0;
  for (let i = open; i < source.length; i += 1) {
    if (source[i] === '{') depth += 1;
    else if (source[i] === '}') {
      depth -= 1;
      if (depth === 0) return source.slice(open + 1, i);
    }
  }
  throw new Error(`${functionName}() body could not be parsed.`);
}

const script = extractScript(html);
new vm.Script(script, { filename: 'index-inline.js' });
new vm.Script(sw, { filename: 'sw.js' });

assert(html.includes('<details') && html.includes('</details>'), 'Preset controls should be hidden in collapsible details sections.');
assert(html.includes('data-i18n="presetSummary"'), 'Preset sections should have translatable summaries.');
assert(html.includes('id="meatPresetControls"'), 'Meat preset controls section is missing.');
assert(html.includes('id="breathMinutesPresetControls"'), 'Breathing practice minute preset controls section is missing.');
assert(html.includes('id="inhalePresetControls"'), 'Inhale preset controls section is missing.');
assert(html.includes('id="exhalePresetControls"'), 'Exhale preset controls section is missing.');

for (let second = 1; second <= 10; second += 1) {
  assert(html.includes(`setBreathPreset('inhale', ${second})`), `Inhale ${second}s preset button is missing.`);
  assert(html.includes(`setBreathPreset('exhale', ${second})`), `Exhale ${second}s preset button is missing.`);
}

assert(countMatches(script, /localStorage\.setItem\('meatMinutes'/g) >= 1, 'Meat minutes should be saved to localStorage.');
assert(countMatches(script, /localStorage\.setItem\('breathMinutes'/g) >= 1, 'Breathing practice minutes should be saved to localStorage.');
assert(countMatches(script, /localStorage\.setItem\('inhaleSeconds'/g) >= 1, 'Inhale seconds should be saved to localStorage.');
assert(countMatches(script, /localStorage\.setItem\('exhaleSeconds'/g) >= 1, 'Exhale seconds should be saved to localStorage.');
assert(script.includes("if (raw === null) return fallback"), 'Missing saved values should preserve default timer values.');
assert(script.includes('loadSavedTimerSettings()'), 'Saved timer settings should be loaded on startup.');
assert(script.includes('saveTimerSettings()'), 'Timer settings save helper should be used.');

const initBody = getFunctionBody(script, 'initLanguage');
assert(initBody.includes('loadSavedTimerSettings()'), 'Saved timer settings should load before language initialization finishes.');

assert(sw.includes("meat-breath-timer-v6"), 'Service worker cache version should be bumped after HTML changes.');

console.log('Smoke checks passed.');
