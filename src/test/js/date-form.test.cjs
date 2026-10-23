const { test } = require('node:test');
const assert = require('node:assert/strict');
const { rules, displayDate, parseDate } = require('../../main/resources/static/js/date-form.js');
const now = new Date('2026-10-11T15:30:45');
const review = {type: 'KLTN', status: 'GRADING', reviewDeadline: '2026-10-30T17:00:00'};

test('KLTN council suggests and restricts the exact period day, not its hour', () => {
    const context = {type: 'KLTN', councilDate: '2026-11-01'};
    assert.match(rules('council', '2026-10-30T08:00', context, now).error, /01\/11\/2026/);
    for (const hour of ['00:00', '08:00', '23:59']) assert.equal(rules('council', '2026-11-01T' + hour, context, now).error, '');
    assert.equal(rules('council', '', context, now).min, '2026-11-01T00:00');
    assert.equal(rules('council', '', context, now).max, '2026-11-01T23:59');
});
test('non-KLTN councils do not inherit a KLTN date restriction', () => {
    const result = rules('council', '2026-10-01T08:00', {type: 'TLCN', councilDate: '2026-11-01'}, now);
    assert.equal(result.error, ''); assert.equal(result.max, '');
});
test('reviewer deadline is strictly future including seconds', () => {
    assert.ok(rules('reviewer', '2026-10-11T15:30', review, now).error);
    assert.equal(rules('reviewer', '2026-10-11T15:31', review, now).error, '');
    assert.equal(rules('reviewer', '', review, now).min, '2026-10-11T15:31');
});
test('reviewer deadline may equal but not exceed period deadline', () => {
    assert.equal(rules('reviewer', '2026-10-30T17:00', review, now).error, '');
    assert.ok(rules('reviewer', '2026-10-30T17:01', review, now).error);
    assert.equal(rules('reviewer', '', review, now).max, '2026-10-30T17:00');
});
test('expired reviewer window and invalid period phase are explained before submit', () => {
    assert.match(rules('reviewer', '', {...review, reviewDeadline: '2026-10-10T17:00'}, now).hint, /đã hết/);
    assert.ok(rules('reviewer', '', {...review, status: 'STUDENT_REGISTRATION'}, now).error);
    assert.equal(rules('reviewer', '', {...review, status: 'IN_PROGRESS'}, now).error, '');
});
test('course reviewer deadline has no artificial period maximum', () => {
    assert.equal(rules('reviewer', '2026-11-01T08:00', {...review, type: 'MON_HOC'}, now).error, '');
    assert.equal(rules('reviewer', '', {...review, type: 'MON_HOC'}, now).max, '');
});
test('reviewer phase messages distinguish unopened and already closed periods', () => {
    for (const status of ['DRAFT', 'LECTURER_REGISTRATION', 'TOPIC_PUBLISHED', 'STUDENT_REGISTRATION']) {
        assert.match(rules('reviewer', '', {...review, status}, now).error, /chưa mở/);
    }
    for (const status of ['RESULT_PUBLISHED', 'CLOSED']) {
        const error = rules('reviewer', '', {...review, status}, now).error;
        assert.match(error, /đã kết thúc/);
        assert.doesNotMatch(error, /chưa mở|IN_PROGRESS|GRADING/);
    }
});
test('announcement draft can have past expiration; publishing cannot', () => {
    assert.equal(rules('announcement', '2026-10-10T10:00', {publish: false}, now).error, '');
    assert.ok(rules('announcement', '2026-10-10T10:00', {publish: true}, now).error);
    assert.equal(rules('announcement', '2026-10-11T15:31', {publish: true}, now).error, '');
    assert.equal(rules('announcement', '', {publish: true}, now).error, '');
});
test('date hints display Vietnamese day/month/year format', () => {
    assert.equal(displayDate('2026-10-30T17:00:00'), '30/10/2026 17:00');
    assert.equal(displayDate('2026-10-30'), '30/10/2026');
});
test('all role date inputs parse day/month/year and reject impossible dates or hours', () => {
    assert.equal(parseDate('20/12/2026 08:00'), '2026-12-20T08:00');
    assert.equal(parseDate('29/02/2028', true), '2028-02-29');
    for (const value of ['12/20/2026 08:00', '29/02/2026 08:00', '31/04/2026 08:00',
        '20/12/2026 24:00', '20/12/2026 08:60', '20/12/2026 08:00 AM']) assert.equal(parseDate(value), null);
});
