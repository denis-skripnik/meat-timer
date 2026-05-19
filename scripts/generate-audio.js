const { execFile } = require('child_process');
const { mkdir } = require('fs/promises');
const path = require('path');

const projectRoot = path.resolve(__dirname, '..');
const audioRoot = path.join(projectRoot, 'assets', 'audio');

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
      'breath-finish': 'Практика дыхания завершена.'
    }
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
      'breath-finish': 'Breathing practice complete.'
    }
  }
};

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
