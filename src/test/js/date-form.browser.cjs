// Run with Playwright installed: node src/test/js/date-form.browser.cjs
// Optionally set BROWSER_EXECUTABLE_PATH to a local Chromium/Chrome executable.
const {chromium} = require('playwright');
const assert = require('node:assert/strict');
const path = require('node:path');
const scripts = path.resolve(__dirname, '../../main/resources/static/js');
(async () => {
    const browser = await chromium.launch({headless: true,
        ...(process.env.BROWSER_EXECUTABLE_PATH ? {executablePath: process.env.BROWSER_EXECUTABLE_PATH} : {})});
    try {
        const page = await browser.newPage();
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        await page.clock.install({time: new Date('2026-10-11T15:30:45')});
        const load = async html => {
            await page.setContent(html);
            await page.addScriptTag({path: path.join(scripts, 'date-form.js')});
        };
        const invalid = async () => page.locator('input[type="datetime-local"]').evaluate(e => e.classList.contains('is-invalid'));
        await load(`<form data-date-form="council"><select data-date-context><option value="">Chọn đợt</option>
            <option value="1" data-type="KLTN" data-council-date="2026-11-01">KLTN</option></select>
            <input name="reportDate" type="datetime-local" required><button>Lưu</button></form>`);
        await page.locator('select').selectOption('1');
        assert.equal(await page.locator('input[type="datetime-local"]').inputValue(), '2026-11-01T08:00');
        assert.equal(await page.locator('input[type="text"]').inputValue(), '01/11/2026 08:00');
        assert.equal(await page.locator('input[type="datetime-local"]').getAttribute('max'), '2026-11-01T23:59');
        await page.locator('input[type="text"]').fill('30/10/2026 08:00');
        assert.equal(await invalid(), true); // Before clicking submit.
        assert.match(await page.locator('.invalid-feedback').textContent(), /01\/11\/2026/);
        await page.evaluate(() => {
            window.blocked = false;
            document.querySelector('form').addEventListener('submit', e => {window.blocked = e.defaultPrevented; e.preventDefault();});
        });
        await page.locator('button').last().click();
        assert.equal(await page.evaluate(() => window.blocked), true);
        await page.locator('input[type="text"]').fill('01/11/2026 23:59');
        assert.equal(await invalid(), false);
        await page.locator('input[type="text"]').fill('31/10/2026 23:59');
        await page.locator('input[type="text"]').press('Enter');
        assert.equal(await page.evaluate(() => window.blocked), true);
        await load(`<form data-date-form="council" data-type="KLTN" data-council-date="2026-11-01">
            <input name="reportDate" type="datetime-local" value="2026-11-01T08:00" required><button>Cập nhật</button></form>`);
        await page.locator('input[type="text"]').fill('02/11/2026 08:00'); assert.equal(await invalid(), true);
        await page.locator('input[type="text"]').fill('01/11/2026 09:00'); assert.equal(await invalid(), false);
        await page.locator('input[type="text"]').fill('01/11/2026 24:00'); assert.equal(await invalid(), true);
        assert.match(await page.locator('.invalid-feedback').textContent(), /ngày, giờ hợp lệ/);

        await load(`<form data-date-form="reviewer"><select data-date-context>
            <option value="1" data-type="KLTN" data-status="GRADING" data-review-deadline="2026-10-30T17:00:00">KLTN</option>
            <option value="2" data-type="KLTN" data-status="DRAFT" data-review-deadline="2026-10-30T17:00:00">Draft</option>
            <option value="3" data-type="KLTN" data-status="CLOSED" data-review-deadline="2026-10-30T17:00:00">Closed</option>
            </select><input name="deadline" type="datetime-local" required><button>Lưu</button></form>`);
        await page.locator('input[type="text"]').fill('11/10/2026 15:30'); assert.equal(await invalid(), true);
        await page.locator('input[type="text"]').fill('30/10/2026 17:01'); assert.equal(await invalid(), true);
        await page.locator('input[type="text"]').fill('30/10/2026 17:00'); assert.equal(await invalid(), false);
        await page.locator('select').selectOption('2'); assert.equal(await invalid(), true);
        assert.match(await page.locator('.invalid-feedback').textContent(), /chưa mở/);
        await page.locator('select').selectOption('3'); assert.equal(await invalid(), true);
        assert.match(await page.locator('.invalid-feedback').textContent(), /đã kết thúc/);
        await page.locator('select').selectOption('1'); assert.equal(await invalid(), false);

        await load(`<form data-date-form="announcement"><input type="datetime-local" name="expirationTime">
            <input type="checkbox" name="publish"><button>Lưu</button></form>`);
        await page.locator('input[type="text"]').fill('10/10/2026 08:00'); assert.equal(await invalid(), false);
        await page.locator('[name="publish"]').check(); assert.equal(await invalid(), true);
        await page.locator('[name="publish"]').uncheck(); assert.equal(await invalid(), false);
        await page.locator('[name="publish"]').check();
        await page.locator('input[type="text"]').fill(''); assert.equal(await invalid(), false);
        await load(`<form data-date-form="announcement" data-published="true">
            <input type="datetime-local" name="expirationTime"><input type="checkbox" name="publish"></form>`);
        await page.locator('input[type="text"]').fill('10/10/2026 08:00');
        assert.equal(await invalid(), true); // Existing published state cannot bypass validation by unchecking.

        const fields = {name: 'text', type: 'select', lecturerStart: 'datetime-local', lecturerEnd: 'datetime-local',
            studentStart: 'datetime-local', studentEnd: 'datetime-local', reportSubmissionDeadline: 'datetime-local',
            reviewDeadline: 'datetime-local', councilDate: 'date'};
        await page.setContent('<form data-period-form>' + Object.entries(fields).map(([name, type]) =>
            `<label for="${name}">${name}</label>` + (type === 'select'
                ? `<select id="${name}" name="${name}"><option value="KLTN">KLTN</option><option value="TLCN">TLCN</option></select>`
                : `<input id="${name}" name="${name}" type="${type}">`) + `<div id="${name}-error"></div>`
        ).join('') + '<button>Lưu</button></form>');
        await page.addScriptTag({path: path.join(scripts, 'period-form.js')});
        await page.locator('#lecturerStart-display').fill('12/10/2026 08:00');
        assert.equal(await page.locator('#lecturerEnd').getAttribute('min'), '2026-10-12T08:01');
        await page.locator('#lecturerEnd-display').fill('12/10/2026 07:00');
        assert.match(await page.locator('#lecturerEnd-error').textContent(), /phải sau/);
        await page.locator('#lecturerEnd-display').fill('12/10/2026 09:00');
        assert.equal(await page.locator('#lecturerEnd-error').textContent(), '');
        await page.locator('#reportSubmissionDeadline-display').fill('30/10/2026 08:00');
        assert.equal(await page.locator('#reviewDeadline').getAttribute('min'), '2026-10-31T00:00');
        await page.locator('#reviewDeadline-display').fill('28/10/2026 08:00');
        assert.match(await page.locator('#reviewDeadline-error').textContent(), /phải sau ngày/);
        await page.locator('#reviewDeadline-display').fill('31/10/2026 08:00');
        assert.equal(await page.locator('#councilDate').getAttribute('min'), '2026-11-01');
        await page.locator('#councilDate-display').fill('31/10/2026');
        assert.match(await page.locator('#councilDate-error').textContent(), /phải sau ngày/);
        await page.locator('#councilDate-display').fill('01/11/2026');
        assert.equal(await page.locator('#councilDate-error').textContent(), '');
        await page.locator('#lecturerStart-display').fill('31/02/2026 08:00');
        assert.match(await page.locator('#lecturerStart-error').textContent(), /ngày hợp lệ/);
        assert.deepEqual(errors, []);
        console.log('Browser checks passed: immediate input/change feedback, dependent limits, suggestions, clearing errors and submit blocking.');
    } finally { await browser.close(); }
})().catch(error => {console.error(error); process.exitCode = 1;});
