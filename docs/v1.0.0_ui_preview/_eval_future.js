document.querySelector('button[data-v="future"]').click();
await new Promise(r => setTimeout(r, 120));
return {
  view: document.querySelector('#v-future').hidden ? 'hidden' : 'visible',
  modCount: document.querySelectorAll('#futureList .fmod').length,
  chipCount: document.querySelectorAll('#futureList .chips span').length
};
