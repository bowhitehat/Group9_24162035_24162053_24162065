/* Immediate feedback mirrors service rules; it never replaces server validation. */
(() => {
    const isoMinute = now => {
        const pad = value => String(value).padStart(2, '0');
        return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}T${pad(now.getHours())}:${pad(now.getMinutes())}`;
    };
    const displayDate = value => !value ? '' : value.slice(0, 10).split('-').reverse().join('/')
        + (value.includes('T') ? ' ' + value.slice(11, 16) : '');
    function parseDate(value, dateOnly = false) {
        const match = value.trim().match(dateOnly ? /^(\d{2})\/(\d{2})\/(\d{4})$/ : /^(\d{2})\/(\d{2})\/(\d{4}) (\d{2}):(\d{2})$/);
        if (!match) return null;
        const [, day, month, year, hour = '00', minute = '00'] = match;
        const leap = +year % 4 === 0 && (+year % 100 !== 0 || +year % 400 === 0);
        const days = [31, leap ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
        if (+year < 1 || +month < 1 || +month > 12 || +day < 1 || +day > days[+month - 1] || +hour > 23 || +minute > 59) return null;
        return `${year}-${month}-${day}` + (dateOnly ? '' : `T${hour}:${minute}`);
    }
    function rules(kind, value, context = {}, now = new Date()) {
        let error = '', hint = '', min = '', max = '';
        if (kind === 'council') {
            if (context.type === 'KLTN' && context.councilDate) {
                min = context.councilDate + 'T00:00';
                max = context.councilDate + 'T23:59';
                hint = `Ngày phải là ${displayDate(context.councilDate)} theo đợt KLTN; chọn giờ báo cáo trong ngày này.`;
                if (value && value.slice(0, 10) !== context.councilDate) error = hint;
            } else hint = 'Chọn đợt trước, sau đó nhập ngày và giờ báo cáo.';
        } else if (kind === 'reviewer') {
            min = isoMinute(new Date(Math.floor(now.getTime() / 60000) * 60000 + 60000));
            max = ['TLCN', 'KLTN'].includes(context.type) ? (context.reviewDeadline || '').slice(0, 16) : '';
            hint = `Hạn nộp điểm phải sau thời điểm hiện tại${max ? ' và không sau ' + displayDate(max) : ''}.`;
            if (value && new Date(value).getTime() <= now.getTime()) error = 'Hạn nộp điểm phải ở tương lai.';
            else if (value && max && new Date(value).getTime() > new Date(context.reviewDeadline).getTime()) error = `Hạn nộp điểm không được sau ${displayDate(max)} của đợt.`;
            if (context.status && !['IN_PROGRESS', 'GRADING'].includes(context.status)) error = 'Đợt chưa cho phép phản biện. Chỉ phân công khi đợt ở IN_PROGRESS hoặc GRADING.';
            if (max && max < min) hint += ' Thời hạn phản biện đã hết; không có thời điểm hợp lệ để phân công mới.';
        } else if (kind === 'announcement') {
            hint = 'Có thể để trống. Khi công bố, thời điểm hết hạn phải ở tương lai.';
            if (context.publish) {
                min = isoMinute(new Date(Math.floor(now.getTime() / 60000) * 60000 + 60000));
                if (value && new Date(value).getTime() <= now.getTime()) error = 'Thời gian hết hạn phải ở tương lai khi công bố thông báo.';
            }
        }
        return { error, hint, min, max };
    }
    function mount(form) {
        const field = form.querySelector('input[type="datetime-local"], input[type="date"]');
        if (!field) return;
        const dateOnly = field.type === 'date';
        const group = document.createElement('div');
        group.className = 'input-group';
        field.replaceWith(group);
        const display = document.createElement('input');
        display.type = 'text';
        display.className = 'form-control';
        display.id = `${field.id || field.name}-display`;
        display.placeholder = dateOnly ? 'dd/MM/yyyy' : 'dd/MM/yyyy HH:mm';
        display.value = displayDate(field.value);
        display.required = field.required;
        display.autocomplete = 'off';
        const label = group.parentElement.querySelector('label');
        if (label) label.htmlFor = display.id;
        const picker = document.createElement('span');
        picker.className = 'position-relative';
        const button = document.createElement('button');
        button.type = 'button';
        button.className = 'btn btn-outline-secondary h-100';
        button.textContent = '📅';
        button.setAttribute('aria-label', 'Chọn lịch: ' + (label?.textContent || field.name));
        field.style.cssText = 'position:absolute;inset:0;width:100%;height:100%;opacity:0;pointer-events:none';
        field.tabIndex = -1;
        field.setAttribute('aria-hidden', 'true');
        picker.append(button, field);
        group.append(display, picker);
        button.addEventListener('click', () => {
            if (typeof field.showPicker === 'function') field.showPicker();
            else { field.style.opacity = '1'; field.style.pointerEvents = 'auto'; field.focus(); }
        });
        const feedback = document.createElement('div');
        feedback.className = 'invalid-feedback d-block';
        feedback.id = `${field.id || field.name}-date-error`;
        feedback.setAttribute('aria-live', 'polite');
        const hint = document.createElement('div');
        hint.className = 'form-text';
        hint.id = `${field.id || field.name}-date-hint`;
        group.after(hint, feedback);
        display.setAttribute('aria-describedby', `${hint.id} ${feedback.id}`);
        let touched = false;
        let parseError = '';
        form.noValidate = true;
        function refresh(showAll = false, suggest = false) {
            const select = form.querySelector('select[data-date-context]');
            const option = select?.selectedOptions[0];
            const context = { ...form.dataset, ...(option?.dataset || {}),
                publish: form.dataset.published === 'true' || !!form.querySelector('[name="publish"]')?.checked };
            if (suggest && form.dataset.dateForm === 'council' && context.type === 'KLTN' && context.councilDate && !field.value) {
                field.value = context.councilDate + 'T08:00';
                display.value = displayDate(field.value);
                parseError = '';
            }
            const result = rules(form.dataset.dateForm, field.value, context);
            field.min = result.min;
            field.max = result.max;
            hint.textContent = result.hint;
            let error = parseError || result.error;
            if (field.validity.badInput) error = 'Vui lòng nhập đầy đủ ngày và giờ hợp lệ.';
            else if (!error && field.required && !field.value && (touched || showAll)) error = 'Vui lòng chọn ngày và giờ.';
            field.setCustomValidity(error);
            display.setCustomValidity(error);
            const visible = !!error && (touched || showAll || !!field.value || !!context.status);
            field.classList.toggle('is-invalid', visible);
            display.classList.toggle('is-invalid', visible);
            display.setAttribute('aria-invalid', String(visible));
            feedback.textContent = visible ? error : '';
            return !error;
        }
        display.addEventListener('input', () => {
            const value = display.value.trim();
            const iso = parseDate(value, dateOnly);
            parseError = value && !iso ? 'Vui lòng nhập đúng định dạng ' + display.placeholder + ' và ngày, giờ hợp lệ.' : '';
            field.value = iso || '';
            touched = true;
            refresh();
        });
        display.addEventListener('blur', () => { touched = true; if (!parseError) display.value = displayDate(field.value); refresh(); });
        for (const event of ['input', 'change', 'blur']) field.addEventListener(event, () => {
            parseError = ''; display.value = displayDate(field.value); touched = true; refresh();
        });
        form.querySelectorAll('select[data-date-context], [name="publish"]').forEach(control => {
            control.addEventListener('change', () => refresh(false, true));
        });
        // Validate before browser constraint validation, including when an open page has aged.
        form.querySelectorAll('button[type="submit"], button:not([type])').forEach(button => button.addEventListener('click', () => refresh(true)));
        form.addEventListener('submit', event => {
            refresh(true);
            if (!form.checkValidity()) {
                event.preventDefault();
                if (!display.checkValidity()) { display.focus(); display.reportValidity(); }
                else form.reportValidity();
            }
        });
        refresh();
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = { rules, isoMinute, displayDate, parseDate };
    if (typeof document !== 'undefined') document.querySelectorAll('form[data-date-form]').forEach(mount);
})();
