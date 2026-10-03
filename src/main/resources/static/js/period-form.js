/* Client-side feedback only; RegistrationPeriodService remains authoritative when saving. */
(() => {
    function validatePeriod(values, now = new Date(), originals = {}) {
        const errors = {};
        const time = value => Date.parse(value);
        const date = value => value.slice(0, 10);
        if (values.lecturerStart && values.lecturerEnd && time(values.lecturerStart) >= time(values.lecturerEnd)) {
            errors.lecturerEnd = 'Thời gian giảng viên kết thúc phải sau thời gian bắt đầu.';
        }
        if (values.studentStart && values.studentEnd && time(values.studentStart) >= time(values.studentEnd)) {
            errors.studentEnd = 'Thời gian sinh viên kết thúc phải sau thời gian bắt đầu.';
        }
        if (values.lecturerEnd && values.studentStart && time(values.lecturerEnd) > time(values.studentStart)) {
            errors.studentStart = 'Sinh viên chỉ đăng ký sau khi giai đoạn giảng viên đã kết thúc.';
        }
        if (values.reportSubmissionDeadline && values.studentEnd && time(values.reportSubmissionDeadline) < time(values.studentEnd)) {
            errors.reportSubmissionDeadline = 'Hạn nộp báo cáo không được trước thời gian sinh viên đăng ký kết thúc.';
        }
        if (['TLCN', 'KLTN'].includes(values.type) && !values.reviewDeadline) {
            errors.reviewDeadline = 'Đợt TLCN/KLTN phải có hạn phản biện.';
        }
        if (values.type === 'KLTN' && !values.councilDate) {
            errors.councilDate = 'Đợt KLTN phải có ngày báo cáo hội đồng.';
        } else if (values.type && values.type !== 'KLTN' && values.councilDate) {
            errors.councilDate = 'Chỉ đợt KLTN được thiết lập ngày báo cáo hội đồng.';
        }
        if (values.reportSubmissionDeadline && values.reviewDeadline
                && date(values.reviewDeadline) <= date(values.reportSubmissionDeadline)) {
            errors.reviewDeadline = 'Hạn phản biện phải sau ngày hạn nộp báo cáo.';
        }
        if (values.type === 'KLTN' && values.councilDate && values.reviewDeadline
                && values.councilDate <= date(values.reviewDeadline)) {
            errors.councilDate = 'Ngày hội đồng phải sau ngày hạn phản biện.';
        }
        const today = [now.getFullYear(), String(now.getMonth() + 1).padStart(2, '0'), String(now.getDate()).padStart(2, '0')].join('-');
        for (const name of ['lecturerStart', 'lecturerEnd', 'studentStart', 'studentEnd', 'reportSubmissionDeadline', 'reviewDeadline']) {
            if (values[name] && date(values[name]) < today
                    && (!originals[name] || date(values[name]) !== date(originals[name]))) {
                errors[name] = 'Không được chọn ngày trong quá khứ. Vui lòng chọn từ hôm nay trở đi.';
            }
        }
        if (values.councilDate && values.councilDate < today && values.councilDate !== originals.councilDate) {
            errors.councilDate = 'Ngày hội đồng không được ở quá khứ.';
        }
        return errors;
    }

    function formatDisplayDate(iso, dateOnly = false) {
        if (!iso) return '';
        const [year, month, day] = iso.slice(0, 10).split('-');
        return `${day}/${month}/${year}` + (dateOnly ? '' : ' ' + iso.slice(11, 16));
    }

    function parseDisplayDate(value, dateOnly = false) {
        const pattern = dateOnly ? /^(\d{2})\/(\d{2})\/(\d{4})$/ : /^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/;
        const match = value.trim().match(pattern);
        if (!match) return null;
        const [, day, month, year, hour = '00', minute = '00'] = match;
        const leapYear = +year % 4 === 0 && (+year % 100 !== 0 || +year % 400 === 0);
        const monthDays = [31, leapYear ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        if (+year < 1 || +month < 1 || +month > 12 || +day < 1 || +day > monthDays[+month - 1]
                || +hour > 23 || +minute > 59) return null;
        return `${year}-${month}-${day}` + (dateOnly ? '' : `T${hour}:${minute}`);
    }

    if (typeof module !== 'undefined' && module.exports) module.exports = { validatePeriod, formatDisplayDate, parseDisplayDate };
    if (typeof document === 'undefined') return;

    document.querySelectorAll('form[data-period-form]').forEach(form => {
        const names = ['name', 'type', 'lecturerStart', 'lecturerEnd', 'studentStart', 'studentEnd',
            'reportSubmissionDeadline', 'reviewDeadline', 'councilDate'];
        const touched = new Set();
        const originals = Object.fromEntries(names.map(name => [name, form.elements.namedItem(name).dataset.originalValue || '']));
        const displays = new Map();
        // Without JavaScript, native required checks and backend validation still apply.
        form.noValidate = true;

        for (const name of names) {
            const field = form.elements.namedItem(name);
            if (!['date', 'datetime-local'].includes(field.type)) continue;
            const dateOnly = field.type === 'date';
            const label = form.querySelector(`label[for="${field.id}"]`);
            const group = document.createElement('div');
            group.className = 'input-group';
            field.replaceWith(group);
            const display = document.createElement('input');
            display.type = 'text';
            display.className = 'form-control';
            display.id = `${field.id}-display`;
            display.placeholder = dateOnly ? 'dd/MM/yyyy' : 'dd/MM/yyyy HH:mm';
            display.value = formatDisplayDate(field.value, dateOnly);
            display.required = field.required;
            display.autocomplete = 'off';
            display.setAttribute('aria-describedby', `${field.id}-error`);
            label.htmlFor = display.id;
            const pickerContainer = document.createElement('span');
            pickerContainer.className = 'position-relative';
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'btn btn-outline-secondary h-100';
            button.textContent = '📅';
            button.setAttribute('aria-label', 'Chọn lịch: ' + label.textContent);
            field.className = '';
            field.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;opacity:0;pointer-events:none';
            field.tabIndex = -1;
            field.setAttribute('aria-hidden', 'true');
            pickerContainer.append(button, field);
            group.append(display, pickerContainer);
            const state = { display, group, dateOnly, error: '' };
            displays.set(name, state);
            button.addEventListener('click', () => {
                if (typeof field.showPicker === 'function') field.showPicker();
                else {
                    field.style.opacity = '1';
                    field.style.pointerEvents = 'auto';
                    field.focus();
                }
            });
            display.addEventListener('input', () => {
                const value = display.value.trim();
                const iso = parseDisplayDate(value, dateOnly);
                state.error = value && !iso ? 'Vui lòng nhập đúng định dạng ' + display.placeholder + ' và ngày hợp lệ.' : '';
                field.value = iso || '';
                touched.add(name);
                refresh();
            });
            display.addEventListener('blur', () => {
                if (field.value && !state.error) display.value = formatDisplayDate(field.value, dateOnly);
                touched.add(name);
                refresh();
            });
            field.addEventListener('input', () => {
                state.error = '';
                display.value = formatDisplayDate(field.value, dateOnly);
            });
            field.addEventListener('change', () => {
                state.error = '';
                display.value = formatDisplayDate(field.value, dateOnly);
            });
        }

        function refresh(showAll = false) {
            const values = Object.fromEntries(names.map(name => [name, form.elements.namedItem(name).value]));
            const errors = validatePeriod(values, new Date(), originals);
            // Restrict the native calendars too; typed values still go through validation.
            const nextDay = value => {
                if (!value) return '';
                const day = new Date(value.slice(0, 10) + 'T00:00:00Z');
                day.setUTCDate(day.getUTCDate() + 1);
                return day.toISOString().slice(0, 10);
            };
            const reviewMin = nextDay(values.reportSubmissionDeadline);
            form.elements.namedItem('reviewDeadline').min = reviewMin ? reviewMin + 'T00:00' : '';
            form.elements.namedItem('councilDate').min = values.type === 'KLTN' ? nextDay(values.reviewDeadline) : '';
            for (const name of names) {
                const field = form.elements.namedItem(name);
                const state = displays.get(name);
                const display = state?.display || field;
                field.setCustomValidity('');
                display.setCustomValidity('');
                let message = state?.error || errors[name] || '';
                if (!message && (field.validity.badInput || field.validity.typeMismatch)) {
                    message = 'Vui lòng nhập ngày, giờ hợp lệ.';
                } else if (!message && ((field.required && !field.value) || (name === 'name' && !field.value.trim()))) {
                    message = 'Vui lòng nhập ' + form.querySelector(`label[for="${display.id}"]`).textContent.toLowerCase() + '.';
                }
                field.setCustomValidity(message);
                display.setCustomValidity(message);
                const visible = !!message && (showAll || touched.has(name) || !!errors[name] || field.validity.badInput);
                display.classList.toggle('is-invalid', visible);
                state?.group.classList.toggle('is-invalid', visible);
                display.setAttribute('aria-invalid', String(visible));
                display.setAttribute('aria-describedby', `${field.id}-error`);
                form.querySelector(`[id="${field.id}-error"]`).textContent = visible ? message : '';
            }
            return form.checkValidity();
        }

        for (const name of names) {
            const field = form.elements.namedItem(name);
            for (const event of ['input', 'change', 'blur']) {
                field.addEventListener(event, () => { touched.add(name); refresh(); });
            }
        }
        form.addEventListener('submit', event => {
            if (!refresh(true)) {
                event.preventDefault();
                form.querySelector('input.is-invalid, select.is-invalid')?.focus();
            }
        });
        refresh();
    });
})();
