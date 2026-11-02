document.querySelector('button[data-v="attr"]').click();
await new Promise(r => setTimeout(r, 120));
return {
  view: document.querySelector('#v-attr').hidden ? 'hidden' : 'visible',
  hiddenAttrCount: document.querySelectorAll('#hiddenAttrs .it').length,
  attrBondsGone: !document.getElementById('attrBonds')
};
