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
assert(html.includes('Три волшебные точки') && html.includes('Валик под поясницей') && html.includes('Валик под грудной клеткой') && html.includes('Валик под шеей'), 'Web breathing guide source text is missing.');
assert(html.includes('setBreathGuideDuration()') && html.includes('speakBreathGuide('), 'Web breathing guide should support 10-minute setup and browser speech.');

for (let second = 1; second <= 10; second += 1) {
  assert(html.includes(`setBreathPreset('inhale', ${second}, this)`), `Inhale ${second}s preset button is missing.`);
  assert(html.includes(`setBreathPreset('exhale', ${second}, this)`), `Exhale ${second}s preset button is missing.`);
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

assert(script.includes('collapsePresetDetails(trigger)'), 'Preset selection should collapse the current details section.');
assert(script.includes("trigger.closest('details')"), 'Preset collapse should target the clicked button details section.');
assert(html.includes('setMeatPreset(5, this)'), 'Meat preset buttons should pass the clicked button to the handler.');
assert(html.includes("setBreathPreset('inhale', 10, this)"), 'Breathing preset buttons should pass the clicked button to the handler.');

assert(script.includes("startBreathPhase('inhale', 650)"), 'Initial inhale phase should start immediately; only the voice prompt may be delayed.');
assert(!script.includes("setTimeout(() => startBreathPhase('inhale'), 650)"), 'Initial phase timing should not be delayed by the start prompt.');
assert(script.includes('promptDelayMs = 0'), 'Breathing phase helper should support delayed prompt without delaying phase timing.');
assert(script.includes('getRemainingSeconds(deadlineMs'), 'Timers should use wall-clock deadlines instead of trusting setInterval cadence.');
assert(script.includes('meatDeadlineMs = Date.now() + meatRemainingSeconds * 1000'), 'Meat timer should resume from a wall-clock deadline.');
assert(script.includes('breathDeadlineMs = breathStartedAtMs + breathConfiguredSeconds * 1000'), 'Breath timer should track a wall-clock deadline.');
assert(script.includes('getBreathPhaseState(elapsedMs, inhaleSeconds, exhaleSeconds)'), 'Breath phases should be recomputed from elapsed wall-clock time.');
assert(script.includes('const TIMER_POLL_MS = 250'), 'Timer state should be polled more often than once per second to avoid late phase transitions.');
assert(script.includes('setInterval(tickBreathTimer, TIMER_POLL_MS)'), 'Breath timer should use the shared high-resolution polling interval.');
assert(script.includes("if (breathTimerDisplay.textContent !== nextText) breathTimerDisplay.textContent = nextText"), 'Breath aria-live timer should update only when the visible second changes.');
assert(script.includes('activeAudioPrompts.add(audio)'), 'Audio prompts should keep a strong reference while playing on mobile browsers.');

const supportUrl = 'https://vtb.paymo.ru/collect-money/qr/?transaction=333eacf7-a897-4e88-8e4e-9be82c929323';
assert(html.includes(supportUrl), 'VTB support payment link is missing.');
assert(html.includes('data-i18n="supportHeading"'), 'Support block heading should be translatable.');
assert(html.includes('class="support-link"'), 'Support CTA should be a normal accessible link.');
assert(html.includes('target="_blank" rel="noopener noreferrer"'), 'External support link should open safely.');

assert(sw.includes("meat-breath-timer-v12"), 'Service worker cache version should be bumped after HTML changes.');

console.log('Smoke checks passed.');
