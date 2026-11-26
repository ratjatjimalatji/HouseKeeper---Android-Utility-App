/* =========================================================
   HouseKeeper · GasCylinder visual (SVG port of PropaneGasCylinderComposable.kt)
   Same proportions as the Compose Canvas: collar 16%, dome 12%, foot ring 9%,
   body width 78%, heights by size (4→190, 9→225, 14→265, 48→305).
   Adds a gas-level overlay (projected remaining gas) on top of the original drawing.
   ========================================================= */
(function () {
  let uid = 0;

  /** Colours and sizes copied from GasCylinder.kt */
  window.HK_CYL_COLORS = [
    ['Propane Red', '#D32F2F'], ['Industrial Blue', '#1976D2'], ['Charcoal Slate', '#37474F'],
    ['Camping Green', '#2E7D32'], ['Safety Orange', '#F57C00'], ['Steel Silver', '#78909C'],
    ['Caution Yellow', '#FBC02D'], ['Deep Teal', '#00796B'], ['Plum Purple', '#6A1B9A']
  ];
  window.HK_CYL_SIZES = [
    { kg: 4, title: '4 kg', use: 'Camping & cooker', days: 20 },
    { kg: 9, title: '9 kg', use: 'BBQ & patio', days: 50 },
    { kg: 14, title: '14 kg', use: 'Household stove', days: 85 },
    { kg: 48, title: '48 kg', use: 'Whole-house heating', days: 240 }
  ];
  const HEIGHT = { 4: 190, 9: 225, 14: 265, 48: 305 };

  function rgb(hex) { const c = hex.replace('#', ''); return [0, 2, 4].map(i => parseInt(c.substr(i, 2), 16)); }
  function darken(hex, f) { return 'rgb(' + rgb(hex).map(v => Math.round(v * (1 - f))).join(',') + ')'; }
  function lighten(hex, f) { return 'rgb(' + rgb(hex).map(v => Math.round(v + (255 - v) * f)).join(',') + ')'; }

  /**
   * hkCylinder({ color, size, level, placeholder, scale, mini })
   *  level: 0..1 remaining gas (null hides the overlay)
   *  placeholder: greyed "new cylinder" with dashed outline (page 0 of the carousel)
   *  mini: list-row icon (no decal, valve detail or shadow)
   */
  window.hkCylinder = function (o) {
    const size = o.size || 9, ph = !!o.placeholder, mini = !!o.mini, scale = o.scale || 1;
    const W = 160, H = HEIGHT[size] || 225, id = 'cy' + (++uid);
    const base = ph ? '#B0BEC5' : (o.color || '#D32F2F');
    const dark = darken(base, ph ? .20 : .35), light = lighten(base, ph ? .20 : .35);
    const dark2 = darken(base, ph ? .32 : .45), dark3 = darken(base, ph ? .44 : .55);
    const bw = W * .78, bl = (W - bw) / 2, br = bl + bw;
    const ch = H * .16, vh = ch * .65, dh = H * .12, fh = H * .09;
    const bt = ch + dh * .65, bb = H - fh - dh * .65, bh = bb - bt;
    const fw = size === 4 ? bw * .9 : size === 48 ? bw * .82 : bw * .85, fl = (W - fw) / 2, ft = H - fh;
    const cx = W / 2, vb = ch + dh * .35, vt = vb - vh;
    const cw = bw * .72, cl = (W - cw) / 2, ct = 4;
    const shadowH = mini ? 0 : 18, VH = H + shadowH;
    const lvl = (o.level == null || ph) ? null : Math.max(0, Math.min(1, o.level));
    const levelY = lvl == null ? null : (lvl <= 0 ? H : bb - bh * lvl);
    const decalY = H - H * .28 - 36;

    let s = `<svg viewBox="0 0 ${W} ${VH}" width="${(W * scale).toFixed(1)}" height="${(VH * scale).toFixed(1)}" role="img" aria-label="${size} kg cylinder${lvl != null ? ', about ' + Math.round(lvl * 100) + '% left' : ''}" style="display:block;overflow:visible">
    <defs>
      <linearGradient id="${id}g" gradientUnits="userSpaceOnUse" x1="${bl}" x2="${br}" y1="0" y2="0">
        <stop offset="0" stop-color="${dark}"/><stop offset=".18" stop-color="${base}"/><stop offset=".35" stop-color="${light}"/>
        <stop offset=".55" stop-color="${base}"/><stop offset=".85" stop-color="${dark2}"/><stop offset="1" stop-color="${dark3}"/></linearGradient>
      <linearGradient id="${id}s" gradientUnits="userSpaceOnUse" x1="${bl + bw * .28}" x2="${bl + bw * .45}" y1="0" y2="0">
        <stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".4" stop-color="#fff" stop-opacity="${ph ? .25 : .45}"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
      <linearGradient id="${id}c" gradientUnits="userSpaceOnUse" x1="${cl}" x2="${cl + cw}" y1="0" y2="0">
        <stop offset="0" stop-color="${dark}"/><stop offset=".3" stop-color="${light}"/><stop offset=".6" stop-color="${base}"/><stop offset="1" stop-color="${dark}"/></linearGradient>
      <linearGradient id="${id}b" x1="0" x2="1"><stop offset="0" stop-color="${ph ? '#78909C' : '#997A15'}"/><stop offset=".5" stop-color="${ph ? '#B0BEC5' : '#D4AF37'}"/><stop offset="1" stop-color="${ph ? '#78909C' : '#997A15'}"/></linearGradient>
      <radialGradient id="${id}f"><stop offset="0" stop-color="#000" stop-opacity="${ph ? .15 : .28}"/><stop offset=".6" stop-color="#000" stop-opacity=".05"/><stop offset="1" stop-color="#000" stop-opacity="0"/></radialGradient>
      <clipPath id="${id}k"><rect x="${bl}" y="${bt}" width="${bw}" height="${bh}"/>
        <rect x="${bl}" y="${ch}" width="${bw}" height="${dh * 1.3}" rx="${bw * .45}" ry="${dh * .65}"/>
        <rect x="${bl}" y="${bb - dh * .4}" width="${bw}" height="${dh * 1.1}" rx="${bw * .45}" ry="${dh * .55}"/></clipPath>
    </defs>`;
    if (!mini) s += `<ellipse cx="${cx}" cy="${H + 9}" rx="65" ry="7" fill="url(#${id}f)"/>`;
    // 1 foot ring
    s += `<rect x="${fl}" y="${ft}" width="${fw}" height="${fh}" rx="6" fill="${dark3}"/>
      <rect x="${fl + 2}" y="${ft + 1}" width="${fw - 4}" height="${fh - 2}" rx="4" fill="url(#${id}g)"/>
      <rect x="${(W - fw * .25) / 2}" y="${ft + fh * .3}" width="${fw * .25}" height="${fh * .4}" rx="3" fill="rgba(0,0,0,.2)"/>`;
    // 2 lower dome, 3 body + shine + welds, 4 upper dome
    s += `<rect x="${bl}" y="${bb - dh * .4}" width="${bw}" height="${dh * 1.1}" rx="${bw * .45}" ry="${dh * .55}" fill="url(#${id}g)"/>
      <rect x="${bl}" y="${bt}" width="${bw}" height="${bh}" fill="url(#${id}g)"/>
      <rect x="${bl + bw * .28}" y="${bt}" width="${bw * .17}" height="${bh}" fill="url(#${id}s)"/>
      <line x1="${bl}" x2="${br}" y1="${bt + bh * .52}" y2="${bt + bh * .52}" stroke="${dark3}" stroke-width="2"/>
      <line x1="${bl}" x2="${br}" y1="${bt + bh * .52 + 2}" y2="${bt + bh * .52 + 2}" stroke="${light}" stroke-opacity=".5" stroke-width="1.2"/>
      <line x1="${bl}" x2="${br}" y1="${bt + 4}" y2="${bt + 4}" stroke="${dark2}" stroke-width="1.5"/>
      <rect x="${bl}" y="${ch}" width="${bw}" height="${dh * 1.3}" rx="${bw * .45}" ry="${dh * .65}" fill="url(#${id}g)"/>
      <rect x="${bl + bw * .28}" y="${ch}" width="${bw * .17}" height="${dh * 1.3}" rx="${bw * .08}" fill="url(#${id}s)"/>`;
    // gas level overlay (empty part faded) + liquid line
    if (levelY != null) {
      s += `<g clip-path="url(#${id}k)"><rect x="${bl}" y="${ch}" width="${bw}" height="${Math.max(0, levelY - ch)}" fill="#fff" fill-opacity=".62"/>
        <rect x="${bl}" y="${ch}" width="${bw}" height="${Math.max(0, levelY - ch)}" fill="url(#${id}s)" fill-opacity=".4"/></g>
        ${lvl > 0 ? `<line x1="${bl - 4}" x2="${br + 4}" y1="${levelY}" y2="${levelY}" stroke="#15191C" stroke-width="2" stroke-dasharray="5 3"/>` : ''}`;
    }
    // 6 collar shroud with handle slots
    s += `<path d="M${cl},${ct + ch + 6} V${ct + 8} Q${cl},${ct} ${cl + 8},${ct} H${cl + cw - 8} Q${cl + cw},${ct} ${cl + cw},${ct + 8} V${ct + ch + 6} Z" fill="url(#${id}c)"/>
      <rect x="${cl}" y="${ct}" width="${cw}" height="3" rx="2" fill="${light}" fill-opacity=".7"/>
      <rect x="${cl + 5}" y="${ct + ch * .32}" width="${cw * .28}" height="${ch * .4}" rx="${ch * .2}" fill="rgba(0,0,0,.24)"/>
      <rect x="${cl + cw - cw * .28 - 5}" y="${ct + ch * .32}" width="${cw * .28}" height="${ch * .4}" rx="${ch * .2}" fill="rgba(0,0,0,.24)"/>`;
    // 5 brass valve (drawn above the shroud so it reads at small sizes)
    if (!mini) s += `<rect x="${cx - 6}" y="${vt + 8}" width="12" height="${vh - 6}" rx="2" fill="url(#${id}b)"/>
      <rect x="${cx + 3}" y="${vt + 12}" width="10" height="7" rx="1" fill="url(#${id}b)"/>
      <circle cx="${cx + 13}" cy="${vt + 15.5}" r="2" fill="#333"/>
      <rect x="${cx - 12}" y="${vt}" width="24" height="7" rx="3" fill="${ph ? '#546E7A' : '#1E1E1E'}"/>`;
    // decal label
    if (!mini) {
      s += ph
        ? `<rect x="${(W - 105) / 2}" y="${decalY}" width="105" height="36" rx="8" fill="#FFF1D2" stroke="#8F5600" stroke-width="1.5"/>
           <text x="${cx}" y="${decalY + 14}" text-anchor="middle" font-family="Roboto,sans-serif" font-size="8.5" font-weight="800" letter-spacing=".5" fill="#8F5600">+ NEW CYLINDER</text>
           <text x="${cx}" y="${decalY + 29}" text-anchor="middle" font-family="Roboto Mono,monospace" font-size="13" font-weight="700" fill="#241800">${size} kg</text>`
        : `<rect x="${(W - 105) / 2}" y="${decalY}" width="105" height="36" rx="8" fill="#fff" fill-opacity=".92"/>
           <text x="${cx}" y="${decalY + 14}" text-anchor="middle" font-family="Roboto,sans-serif" font-size="8.5" font-weight="800" letter-spacing="1" fill="#C62828">CYLINDER SIZE</text>
           <text x="${cx}" y="${decalY + 30}" text-anchor="middle" font-family="Roboto Mono,monospace" font-size="15" font-weight="700" fill="#212121">${size} kg</text>`;
    }
    if (ph) s += `<rect x="${bl - 3}" y="${ct - 2}" width="${bw + 6}" height="${H - ct}" rx="16" fill="none" stroke="#8F5600" stroke-width="2.5" stroke-dasharray="12 8"/>`;
    return s + '</svg>';
  };

  /** Gas maths shared by both pages. rate = kg/day learned from finished cylinders. */
  window.hkGasProjection = function (cyl, rate, daysInUse) {
    const used = rate * daysInUse;
    const leftKg = Math.max(0, cyl.size - used);
    return { leftKg, level: leftKg / cyl.size, daysLeft: Math.floor(leftKg / rate) };
  };
})();
