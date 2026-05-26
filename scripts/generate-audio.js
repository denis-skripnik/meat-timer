const { execFile } = require('child_process');
const { mkdir } = require('fs/promises');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const audioRoot = path.join(projectRoot, 'assets', 'audio');

const ruNumbers = {
  1: 'одна', 2: 'две', 3: 'три', 4: 'четыре', 5: 'пять', 6: 'шесть', 7: 'семь', 8: 'восемь', 9: 'девять',
  10: 'десять', 11: 'одиннадцать', 12: 'двенадцать', 13: 'тринадцать', 14: 'четырнадцать', 15: 'пятнадцать',
  16: 'шестнадцать', 17: 'семнадцать', 18: 'восемнадцать', 19: 'девятнадцать', 20: 'двадцать',
  30: 'тридцать', 40: 'сорок', 50: 'пятьдесят', 60: 'шестьдесят', 70: 'семьдесят', 80: 'восемьдесят', 90: 'девяносто', 100: 'сто'
};

const enOnes = ['', 'one', 'two', 'three', 'four', 'five', 'six', 'seven', 'eight', 'nine'];
const enTeens = ['ten', 'eleven', 'twelve', 'thirteen', 'fourteen', 'fifteen', 'sixteen', 'seventeen', 'eighteen', 'nineteen'];
const enTens = ['', '', 'twenty', 'thirty', 'forty', 'fifty', 'sixty'];

function ruNumber(n) {
  if (ruNumbers[n]) return ruNumbers[n];
  if (n > 100) return `сто ${ruNumber(n - 100)}`;
  const tens = Math.floor(n / 10) * 10;
  return `${ruNumbers[tens]} ${ruNumbers[n % 10]}`;
}

function enNumber(n) {
  if (n < 10) return enOnes[n];
  if (n < 20) return enTeens[n - 10];
  if (n < 100) {
    const tens = Math.floor(n / 10);
    const one = n % 10;
    return one ? `${enTens[tens]} ${enOnes[one]}` : enTens[tens];
  }
  return n === 100 ? 'one hundred' : `one hundred ${enNumber(n - 100)}`;
}

const prompts = {
  ru: {
    voice: 'ru-RU-SvetlanaNeural',
    rate: '+0%',
    items: {
      'meat-start': 'Таймер готовки запущен.',
      'meat-minute': 'Прошла минута. Пора перевернуть мясо.',
      'meat-finish': 'Готовка завершена. Можно снимать мясо с огня.',
      'breath-start': 'Практика дыхания началась.',
      'breath-inhale': 'Вдох.',
      'breath-exhale': 'Выдох.',
      'breath-minute': 'Прошла минута. Продолжайте спокойно дышать.',
      'breath-finish': 'Практика дыхания завершена.',
      'minute-elapsed-one': 'Прошла',
      'minute-elapsed-many': 'Прошло',
      'minute-one': 'минута.',
      'minute-few': 'минуты.',
      'minute-many': 'минут.',
      'meat-minute-action': 'Пора перевернуть мясо.',
      'breath-minute-action': 'Продолжайте спокойно дышать.'
    },
    number: ruNumber
  },
  en: {
    voice: 'en-US-AriaNeural',
    rate: '+0%',
    items: {
      'meat-start': 'Cooking timer started.',
      'meat-minute': 'One minute passed. Time to flip the meat.',
      'meat-finish': 'Cooking complete. You can remove the meat from heat.',
      'breath-start': 'Breathing practice started.',
      'breath-inhale': 'Inhale.',
      'breath-exhale': 'Exhale.',
      'breath-minute': 'One minute passed. Keep breathing calmly.',
      'breath-finish': 'Breathing practice complete.',
      'minute-elapsed': 'passed.',
      'minute-one': 'minute',
      'minute-many': 'minutes',
      'meat-minute-action': 'Time to flip the meat.',
      'breath-minute-action': 'Keep breathing calmly.'
    },
    number: enNumber
  }
};

for (const config of Object.values(prompts)) {
  for (let n = 1; n <= 120; n += 1) config.items[`number-${n}`] = config.number(n);
}

function runEdgeTts(args) {
  return new Promise((resolve, reject) => {
    execFile('edge-tts', args, { cwd: projectRoot }, (error, stdout, stderr) => {
      if (error) {
        reject(new Error(`${error.message}\n${stderr || stdout}`));
        return;
      }
      resolve({ stdout, stderr });
    });
  });
}

async function generate() {
  for (const [lang, config] of Object.entries(prompts)) {
    const langDir = path.join(audioRoot, lang);
    await mkdir(langDir, { recursive: true });

    for (const [key, text] of Object.entries(config.items)) {
      const output = path.join(langDir, `${key}.mp3`);
      await runEdgeTts([
        '--voice', config.voice,
        '--rate', config.rate,
        '--text', text,
        '--write-media', output
      ]);
      console.log(`generated ${path.relative(projectRoot, output)}`);
    }
  }
}

generate().catch(error => {
  console.error('Audio generation failed. Check edge-tts installation/network access.');
  console.error(error.message);
  process.exit(1);
});
