// Execute the real inline application with deterministic clock/audio adapters.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const { test } = require('node:test');
const root = path.resolve(__dirname, '..');
const source = fs.readFileSync(path.join(root, 'index.html'), 'utf8').match(/<script>([\s\S]*?)<\/script>/)[1];

function app() {
  let now = 100000, id = 0;
  const tasks = new Map(), sounds = [], alerts = [], nodes = new Map();
  const storage = new Map();
  const defaults = { meatMinutesInput: '10', breathMinutesInput: '5', inhaleInput: '4', exhaleInput: '6' };
  function node(name) {
    if (!nodes.has(name)) nodes.set(name, {
      value: defaults[name] || '', min: '1', max: name.includes('hale') ? '30' : '120', step: '1',
      checked: false, textContent: '', style: {}, classList: { add() {}, remove() {}, toggle() {} },
      addEventListener() {}, setAttribute() {}
    });
    return nodes.get(name);
  }
  class Audio {
    constructor(url) { this.url = url; this.listeners = {}; this.playing = false; sounds.push(this); }
    addEventListener(event, f) { (this.listeners[event] ||= []).push(f); }
    removeEventListener(event, f) { this.listeners[event] = (this.listeners[event] || []).filter(x => x !== f); }
    play() { this.playing = true; return Promise.resolve(); }
    pause() { this.playing = false; }
    end() { this.playing = false; for (const f of this.listeners.ended || []) f(); }
  }
  const ctx = vm.createContext({
    console, Date: { now: () => now }, Audio,
    document: { getElementById: node, addEventListener() {}, querySelectorAll: () => [], documentElement: {} },
    navigator: { language: 'ru' }, window: { addEventListener() {} },
    localStorage: { getItem: k => storage.get(k) ?? null, setItem: (k, v) => storage.set(k, String(v)) },
    setInterval: () => ++id, clearInterval() {},
    setTimeout: (f, ms) => { tasks.set(++id, { f, at: now + ms }); return id; },
    clearTimeout: key => tasks.delete(key), alert: msg => alerts.push(msg)
  });
  const run = js => vm.runInContext(js, ctx);
  run(source);
  return { node, run, sounds, alerts, advance(ms) { now += ms; for (const [key, task] of [...tasks]) if (task.at <= now) { tasks.delete(key); task.f(); } } };
}

test('reject invalid integer durations and phase settings, accept boundaries', () => {
  for (const v of ['0', '121', '1.5', '', '-1', 'Infinity']) {
    const a = app(); a.node('meatMinutesInput').value = v; a.run('startMeatTimer()');
    assert.equal(a.run('meatInterval'), null, v);
    assert.equal(a.alerts.length, 1);
  }
  for (const v of ['1', '120']) {
    const a = app(); a.node('meatMinutesInput').value = v; a.run('startMeatTimer()');
    assert.ok(a.run('meatInterval')); assert.equal(a.run('meatRemainingSeconds'), Number(v) * 60);
  }
  for (const v of ['0', '31', '1.5', '']) {
    const a = app(); a.node('inhaleInput').value = v; a.run('startBreathTimer()');
    assert.equal(a.run('breathInterval'), null, v);
  }
});

test('invalid live edits cannot freeze completion; retain last valid phase', () => {
  const a = app(); a.node('breathMinutesInput').value = '1'; a.run('startBreathTimer()');
  a.node('inhaleInput').value = ''; a.advance(5000); a.run('tickBreathTimer()');
  assert.equal(a.run('breathRemainingSeconds'), 55);
  assert.equal(a.run('breathPhase'), 'exhale');
  a.node('exhaleInput').value = '0'; a.advance(55000); a.run('tickBreathTimer()');
  assert.equal(a.run('breathInterval'), null); assert.equal(a.run('breathRemainingSeconds'), 0);
});

test('pause preserves total elapsed/minute progression and breathing phase', () => {
  const a = app(); a.run('startBreathTimer()'); a.advance(65500); a.run('tickBreathTimer(); pauseBreathTimer()');
  a.advance(30000); a.run('startBreathTimer(); tickBreathTimer()');
  assert.equal(a.run('breathPhase'), 'exhale');
  assert.equal(a.run('breathRemainingSeconds'), 235);
  a.advance(55000); a.run('tickBreathTimer()'); assert.equal(a.run('breathLastAnnouncedMinute'), 2);
});

test('start and phase never overlap; phase reflects current clock after start clip', () => {
  const a = app(); a.run('startBreathTimer()'); a.advance(5000); a.run('tickBreathTimer()');
  assert.equal(a.sounds.filter(x => x.playing).length, 1);
  a.sounds.find(x => x.playing).end(); a.run('tickBreathTimer()');
  assert.equal(a.sounds.filter(x => x.playing).length, 1);
  assert.ok(a.sounds.find(x => x.playing).url.endsWith('breath-exhale.mp3'));
});

test('reset cancels active audio and delayed prompts, but not the other timer', () => {
  const a = app(); a.run('startBreathTimer(); resetBreathTimer()'); a.advance(2000);
  assert.equal(a.sounds.filter(x => x.playing).length, 0);
  a.run('startMeatTimer(); startBreathTimer(); resetBreathTimer()');
  assert.equal(a.sounds.filter(x => x.playing).length, 1);
  assert.ok(a.sounds.find(x => x.playing).url.endsWith('meat-start.mp3'));
  a.run('resetMeatTimer()'); assert.equal(a.sounds.filter(x => x.playing).length, 0);
});

test('minute queue stops on pause and finish replaces obsolete speech', () => {
  const a = app(); a.run('startMeatTimer()'); a.sounds[0].end();
  a.advance(60000); a.run('tickMeatTimer()'); a.advance(1000);
  assert.equal(a.sounds.filter(x => x.playing).length, 1);
  const old = a.sounds.find(x => x.playing); a.run('pauseMeatTimer()'); old.end();
  assert.equal(a.sounds.filter(x => x.playing).length, 0);
  a.run('resetMeatTimer()'); a.node('meatMinutesInput').value = '1'; a.run('startMeatTimer()');
  a.advance(60000); a.run('tickMeatTimer()'); a.advance(1200);
  assert.equal(a.sounds.filter(x => x.playing).length, 1);
  assert.ok(a.sounds.find(x => x.playing).url.endsWith('meat-finish.mp3'));
});

test('new run cancels delayed completion from the previous run', () => {
  for (const kind of ['Meat', 'Breath']) {
    const a = app();
    a.run(`${kind.toLowerCase()}MinutesInput.value='1'; start${kind}Timer()`);
    a.advance(60000); a.run(`tick${kind}Timer(); start${kind}Timer()`);
    a.advance(1500);
    assert.ok(!a.sounds.some(sound => sound.url.endsWith('-finish.mp3')));
    assert.equal(a.sounds.filter(sound => sound.playing).length, 1);
  }
});

test('English generator covers the full supported range without undefined text', () => {
  // Never invoke TTS: importing the generator must have no generation side effects.
  const sandbox = { require: name => name === 'child_process' ? { execFile() { throw Error('TTS must not run'); } } : require(name), __dirname: path.join(root, 'scripts'), module: { exports: {} }, console };
  const code = fs.readFileSync(path.join(root, 'scripts/generate-audio.js'), 'utf8');
  vm.createContext(sandbox); vm.runInContext(code.replace(/generate\(\)\.catch\([\s\S]*$/, ''), sandbox);
  for (let n = 1; n <= 120; n++) assert.match(vm.runInContext(`enNumber(${n})`, sandbox) || '', /^(?!.*undefined)[a-z]+(?: [a-z]+)*$/);
  for (const [n, word] of [[70, 'seventy'], [71, 'seventy one'], [80, 'eighty'], [99, 'ninety nine'], [120, 'one hundred twenty']]) assert.equal(vm.runInContext(`enNumber(${n})`, sandbox), word);
});
