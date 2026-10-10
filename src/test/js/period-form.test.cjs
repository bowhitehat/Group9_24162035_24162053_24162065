const { test } = require('node:test');
const assert = require('node:assert/strict');
const { validatePeriod: validatePeriodAt, formatDisplayDate, parseDisplayDate } = require('../../main/resources/static/js/period-form.js');
const validatePeriod = values => validatePeriodAt(values, new Date('2026-08-01T10:00:00'));

const valid = {
    name: 'Đợt TLCN', type: 'TLCN', lecturerStart: '2026-08-28T15:09', lecturerEnd: '2026-10-02T15:09',
    studentStart: '2026-10-03T15:10', studentEnd: '2026-10-30T15:10',
    reportSubmissionDeadline: '2026-11-01T15:10', reviewDeadline: '2026-11-10T15:10', councilDate: ''
};

test('valid schedule has no warnings', () => assert.deepEqual(validatePeriod(valid), {}));
test('screenshot deadline is rejected immediately', () => {
    assert.match(validatePeriod({ ...valid, reportSubmissionDeadline: '2026-09-18T15:10' }).reportSubmissionDeadline, /không được trước/);
});
test('fixing the deadline removes warning', () => {
    const values = { ...valid, reportSubmissionDeadline: '2026-09-18T15:10' };
    assert.ok(validatePeriod(values).reportSubmissionDeadline);
    values.reportSubmissionDeadline = values.studentEnd;
    assert.deepEqual(validatePeriod(values), {});
});
test('equal or reversed window ends are rejected', () => {
    assert.ok(validatePeriod({ ...valid, lecturerEnd: valid.lecturerStart }).lecturerEnd);
    assert.ok(validatePeriod({ ...valid, studentEnd: valid.studentStart }).studentEnd);
});
test('equivalent minute and second values count as equal', () => {
    assert.ok(validatePeriod({ ...valid, lecturerEnd: valid.lecturerStart + ':00' }).lecturerEnd);
});
test('overlapping lecturer and student windows are rejected', () => {
    assert.ok(validatePeriod({ ...valid, studentStart: '2026-10-01T15:09' }).studentStart);
    assert.deepEqual(validatePeriod({ ...valid, studentStart: valid.lecturerEnd }), {});
});
test('changing student end rechecks an existing report deadline', () => {
    assert.ok(validatePeriod({ ...valid, studentEnd: '2026-11-02T15:10' }).reportSubmissionDeadline);
});
test('TLCN and KLTN require review deadline', () => {
    for (const type of ['TLCN', 'KLTN']) assert.ok(validatePeriod({ ...valid, type, reviewDeadline: '' }).reviewDeadline);
    assert.equal(validatePeriod({ ...valid, type: 'MON_HOC', reviewDeadline: '' }).reviewDeadline, undefined);
});
test('TLCN and KLTN cannot omit report deadline to bypass the milestone chain', () => {
    for (const type of ['TLCN', 'KLTN']) {
        assert.match(validatePeriod({ ...valid, type, reportSubmissionDeadline: '', councilDate: type === 'KLTN' ? '2026-11-11' : '' })
            .reportSubmissionDeadline, /phải có thời hạn nộp báo cáo/);
    }
});
test('course and research periods may omit report deadline', () => {
    for (const type of ['MON_HOC', 'NCKH']) {
        assert.deepEqual(validatePeriod({ ...valid, type, reportSubmissionDeadline: '', reviewDeadline: '' }), {});
    }
});
test('council date is required only for KLTN', () => {
    assert.ok(validatePeriod({ ...valid, type: 'KLTN' }).councilDate);
    assert.ok(validatePeriod({ ...valid, councilDate: '2026-11-20' }).councilDate);
    assert.deepEqual(validatePeriod({ ...valid, type: 'KLTN', councilDate: '2026-11-20' }), {});
});
test('partial empty form does not produce cross-field warnings', () => assert.deepEqual(validatePeriod({}), {}));

test('screenshot past lecturer dates are rejected even with correct ordering', () => {
    const errors = validatePeriodAt({ ...valid, lecturerStart: '2026-09-18T15:34', lecturerEnd: '2026-09-24T15:34' }, new Date('2026-10-01T15:34:00'));
    assert.match(errors.lecturerStart, /quá khứ/);
    assert.match(errors.lecturerEnd, /quá khứ/);
});
test('every timestamp field rejects a past value', () => {
    for (const name of ['lecturerStart', 'lecturerEnd', 'studentStart', 'studentEnd', 'reportSubmissionDeadline', 'reviewDeadline']) {
        const errors = validatePeriodAt({ ...valid, [name]: '2026-07-01T10:00' }, new Date('2026-08-01T10:00:00'));
        assert.match(errors[name], /quá khứ/);
    }
});
test('fixing a past value clears its warning', () => {
    const now = new Date('2026-08-29T10:00:00');
    assert.ok(validatePeriodAt(valid, now).lecturerStart);
    assert.deepEqual(validatePeriodAt({ ...valid, lecturerStart: '2026-08-30T10:00' }, now), {});
});
test('today is accepted even if the selected hour has already passed', () => {
    assert.deepEqual(validatePeriodAt({ ...valid, lecturerStart: '2026-08-01T00:00' }, new Date('2026-08-01T23:59:45')), {});
    assert.deepEqual(validatePeriodAt({ ...valid, type: 'MON_HOC', reportSubmissionDeadline: '', reviewDeadline: '2026-08-01T00:00' }, new Date('2026-08-01T23:59:45')), {});
});
test('unchanged historical dates are allowed on edit but a different past date is rejected', () => {
    const now = new Date('2026-10-01T10:00:00');
    assert.deepEqual(validatePeriodAt(valid, now, { lecturerStart: valid.lecturerStart + ':25' }), {});
    assert.deepEqual(validatePeriodAt({ ...valid, lecturerStart: '2026-08-28T00:00' }, now, valid), {});
    assert.match(validatePeriodAt({ ...valid, lecturerStart: '2026-08-27T10:00' }, now, valid).lecturerStart, /quá khứ/);
});
test('past council date is rejected, today and original dates are accepted', () => {
    const now = new Date('2026-08-01T10:00:00');
    const values = { ...valid, type: 'KLTN', lecturerStart: '2026-07-01T10:00', lecturerEnd: '2026-07-05T10:00',
        studentStart: '2026-07-06T10:00', studentEnd: '2026-07-10T10:00',
        reportSubmissionDeadline: '2026-07-20T10:00', reviewDeadline: '2026-07-30T10:00', councilDate: '2026-07-31' };
    const originals = { ...values, councilDate: '' };
    assert.match(validatePeriodAt(values, now, originals).councilDate, /quá khứ/);
    assert.deepEqual(validatePeriodAt({ ...values, councilDate: '2026-08-01' }, now, originals), {});
    assert.deepEqual(validatePeriodAt(values, now, values), {});
});

test('review deadline rejects an earlier day and the report day even at a later hour', () => {
    for (const reviewDeadline of ['2026-10-28T08:18', '2026-10-30T23:59']) {
        const errors = validatePeriod({ ...valid, type: 'KLTN', reportSubmissionDeadline: '2026-10-30T15:10',
            reviewDeadline, councilDate: '2026-11-20' });
        assert.match(errors.reviewDeadline, /phải sau ngày hạn nộp báo cáo/);
    }
});
test('council date must be strictly after the review day', () => {
    for (const councilDate of ['2026-11-09', '2026-11-10']) {
        assert.match(validatePeriod({ ...valid, type: 'KLTN', councilDate }).councilDate, /phải sau ngày hạn phản biện/);
    }
    assert.deepEqual(validatePeriod({ ...valid, type: 'KLTN', councilDate: '2026-11-11' }), {});
});
test('a later day is valid even at an earlier hour', () => {
    assert.deepEqual(validatePeriod({ ...valid, type: 'KLTN', reportSubmissionDeadline: '2026-11-01T23:59',
        reviewDeadline: '2026-11-02T00:00', councilDate: '2026-11-03' }), {});
});
test('changing earlier milestones rechecks the rest of the schedule', () => {
    const values = { ...valid, type: 'KLTN', councilDate: '2026-11-11' };
    values.reportSubmissionDeadline = '2026-11-12T10:00';
    assert.ok(validatePeriod(values).reviewDeadline);
    values.reviewDeadline = '2026-11-13T10:00';
    assert.ok(validatePeriod(values).councilDate);
    values.councilDate = '2026-11-14';
    assert.deepEqual(validatePeriod(values), {});
});
test('invalid historical ordering is not exempted when editing', () => {
    const values = { ...valid, type: 'KLTN', reportSubmissionDeadline: '2026-11-12T10:00', councilDate: '2026-11-09' };
    const errors = validatePeriodAt(values, new Date('2026-12-01T10:00'), values);
    assert.match(errors.reviewDeadline, /phải sau ngày hạn nộp báo cáo/);
    assert.match(errors.councilDate, /phải sau ngày hạn phản biện/);
});

test('dates display day/month/year regardless of browser locale', () => {
    assert.equal(formatDisplayDate('2026-10-03T15:39'), '03/10/2026 15:39');
    assert.equal(formatDisplayDate('2026-10-03', true), '03/10/2026');
    assert.equal(formatDisplayDate(''), '');
});
test('display dates are converted to ISO for backend binding', () => {
    assert.equal(parseDisplayDate('03/10/2026 15:39'), '2026-10-03T15:39');
    assert.equal(parseDisplayDate('03/10/2026', true), '2026-10-03');
    assert.equal(parseDisplayDate(' 03/10/2026 00:00 '), '2026-10-03T00:00');
});
test('impossible dates and times do not silently roll over', () => {
    for (const value of ['31/04/2026 15:00', '29/02/2026 15:00', '03/10/2026 24:00', '03/10/2026 15:60', '10/13/2026 15:00', '2026-10-03T15:00']) {
        assert.equal(parseDisplayDate(value), null);
    }
    assert.equal(parseDisplayDate('29/02/2028 15:00'), '2028-02-29T15:00');
});
