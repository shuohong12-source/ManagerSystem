(function () {
    const data = window.dashboardData || {};
    const colors = ["#1d5f8f", "#0f8f7a", "#c78116", "#b83a2f", "#5d6bb3", "#23869b"];

    function numberValue(row, key) {
        const value = row && row[key];
        if (value === null || value === undefined || value === "") {
            return 0;
        }
        return Number(value);
    }

    function formatNumber(value) {
        return Number(value || 0).toLocaleString("zh-CN", { maximumFractionDigits: 2 });
    }

    function clear(el) {
        if (el) {
            el.innerHTML = "";
        }
    }

    function tooltip(container) {
        let tip = container.querySelector(".chart-tooltip");
        if (!tip) {
            tip = document.createElement("div");
            tip.className = "chart-tooltip";
            container.appendChild(tip);
        }
        return {
            show(event, html) {
                tip.innerHTML = html;
                tip.style.left = `${event.offsetX + 14}px`;
                tip.style.top = `${event.offsetY + 14}px`;
                tip.classList.add("show");
            },
            hide() {
                tip.classList.remove("show");
            }
        };
    }

    function svg(width, height) {
        return `<svg viewBox="0 0 ${width} ${height}" role="img" aria-hidden="true">`;
    }

    function renderRegionChart(mode) {
        const container = document.getElementById("regionChart");
        if (!container) return;
        clear(container);
        const rows = data.regionVolumes || [];
        const key = mode === "freight" ? "total_freight" : "total_quantity";
        const unit = mode === "freight" ? "元" : "件";
        const max = Math.max(...rows.map(row => numberValue(row, key)), 1);
        const chartTip = tooltip(container);
        let html = svg(720, 300);
        rows.forEach((row, index) => {
            const value = numberValue(row, key);
            const width = Math.max(6, (value / max) * 470);
            const y = 40 + index * 58;
            html += `
                <text x="24" y="${y + 22}" class="chart-label">${row.region_name || ""}</text>
                <rect x="160" y="${y}" width="500" height="30" rx="8" class="track"></rect>
                <rect class="interactive-bar" data-label="${row.region_name || ""}" data-value="${formatNumber(value)} ${unit}"
                      x="160" y="${y}" width="${width}" height="30" rx="8" fill="${colors[index % colors.length]}"></rect>
                <text x="${Math.min(650, 172 + width)}" y="${y + 21}" class="bar-value">${formatNumber(value)}${unit}</text>`;
        });
        html += "</svg>";
        container.insertAdjacentHTML("afterbegin", html);
        container.querySelectorAll(".interactive-bar").forEach(bar => {
            bar.addEventListener("mousemove", event => {
                chartTip.show(event, `<strong>${bar.dataset.label}</strong><span>${bar.dataset.value}</span>`);
            });
            bar.addEventListener("mouseleave", chartTip.hide);
        });
    }

    function renderGoodsChart() {
        const container = document.getElementById("goodsChart");
        if (!container) return;
        clear(container);
        const rows = (data.topGoods || []).slice(0, 8);
        const max = Math.max(...rows.map(row => numberValue(row, "declared_value")), 1);
        const chartTip = tooltip(container);
        let html = svg(520, 360);
        rows.forEach((row, index) => {
            const value = numberValue(row, "declared_value");
            const width = Math.max(8, (value / max) * 300);
            const y = 28 + index * 40;
            html += `
                <text x="18" y="${y + 18}" class="chart-label small">${row.goods_name || ""}</text>
                <rect x="170" y="${y}" width="310" height="22" rx="6" class="track"></rect>
                <rect class="interactive-bar" data-label="${row.goods_name || ""}" data-value="申报货值 ${formatNumber(value)} 元，数量 ${formatNumber(numberValue(row, "ship_quantity"))} 件"
                      x="170" y="${y}" width="${width}" height="22" rx="6" fill="${colors[index % colors.length]}"></rect>`;
        });
        html += "</svg>";
        container.insertAdjacentHTML("afterbegin", html);
        container.querySelectorAll(".interactive-bar").forEach(bar => {
            bar.addEventListener("mousemove", event => {
                chartTip.show(event, `<strong>${bar.dataset.label}</strong><span>${bar.dataset.value}</span>`);
            });
            bar.addEventListener("mouseleave", chartTip.hide);
        });
    }

    function polarToCartesian(cx, cy, r, angle) {
        const rad = (angle - 90) * Math.PI / 180;
        return { x: cx + r * Math.cos(rad), y: cy + r * Math.sin(rad) };
    }

    function arcPath(cx, cy, r, startAngle, endAngle) {
        const start = polarToCartesian(cx, cy, r, endAngle);
        const end = polarToCartesian(cx, cy, r, startAngle);
        const largeArc = endAngle - startAngle <= 180 ? "0" : "1";
        return `M ${start.x} ${start.y} A ${r} ${r} 0 ${largeArc} 0 ${end.x} ${end.y}`;
    }

    function renderStatusChart() {
        const container = document.getElementById("statusChart");
        const legend = document.getElementById("statusLegend");
        if (!container || !legend) return;
        clear(container);
        clear(legend);
        const rows = data.statusDistribution || [];
        const total = rows.reduce((sum, row) => sum + numberValue(row, "count_value"), 0) || 1;
        const chartTip = tooltip(container);
        let angle = 0;
        let html = svg(260, 240);
        rows.forEach((row, index) => {
            const value = numberValue(row, "count_value");
            const next = angle + value / total * 360;
            html += `<path class="donut-slice" d="${arcPath(130, 115, 76, angle, next)}"
                     stroke="${colors[index % colors.length]}" data-label="${row.status}" data-value="${value}" />`;
            angle = next;
            const item = document.createElement("span");
            item.innerHTML = `<i style="background:${colors[index % colors.length]}"></i>${row.status} ${value}`;
            legend.appendChild(item);
        });
        html += `<text x="130" y="108" text-anchor="middle" class="donut-total">${formatNumber(total)}</text>
                 <text x="130" y="132" text-anchor="middle" class="donut-caption">总运单</text></svg>`;
        container.insertAdjacentHTML("afterbegin", html);
        container.querySelectorAll(".donut-slice").forEach(slice => {
            slice.addEventListener("mousemove", event => {
                chartTip.show(event, `<strong>${slice.dataset.label}</strong><span>${slice.dataset.value} 单</span>`);
            });
            slice.addEventListener("mouseleave", chartTip.hide);
        });
    }

    function renderTrendChart() {
        const container = document.getElementById("trendChart");
        if (!container) return;
        clear(container);
        const rows = data.monthlyTrend || [];
        const maxCount = Math.max(...rows.map(row => numberValue(row, "waybill_count")), 1);
        const maxFreight = Math.max(...rows.map(row => numberValue(row, "freight_amount")), 1);
        const chartTip = tooltip(container);
        const points = rows.map((row, index) => {
            const x = rows.length === 1 ? 270 : 42 + index * (430 / (rows.length - 1));
            const y = 260 - numberValue(row, "waybill_count") / maxCount * 180;
            return { x, y, row };
        });
        let html = svg(540, 320);
        html += `<line x1="42" y1="260" x2="500" y2="260" class="axis"></line>
                 <line x1="42" y1="50" x2="42" y2="260" class="axis"></line>`;
        if (points.length) {
            html += `<polyline points="${points.map(p => `${p.x},${p.y}`).join(" ")}" class="trend-line"></polyline>`;
        }
        points.forEach((point, index) => {
            const freightBar = numberValue(point.row, "freight_amount") / maxFreight * 70;
            html += `
                <rect x="${point.x - 10}" y="${260 - freightBar}" width="20" height="${freightBar}" rx="4" class="trend-bar"></rect>
                <circle class="trend-dot" cx="${point.x}" cy="${point.y}" r="6"
                        data-label="${point.row.month_value}" data-value="运单 ${formatNumber(numberValue(point.row, "waybill_count"))} 单，运费 ${formatNumber(numberValue(point.row, "freight_amount"))} 元"></circle>
                <text x="${point.x}" y="292" text-anchor="middle" class="chart-label small">${point.row.month_value}</text>`;
        });
        html += "</svg>";
        container.insertAdjacentHTML("afterbegin", html);
        container.querySelectorAll(".trend-dot").forEach(dot => {
            dot.addEventListener("mousemove", event => {
                chartTip.show(event, `<strong>${dot.dataset.label}</strong><span>${dot.dataset.value}</span>`);
            });
            dot.addEventListener("mouseleave", chartTip.hide);
        });
    }

    document.querySelectorAll("[data-region-mode] button").forEach(button => {
        button.addEventListener("click", () => {
            document.querySelectorAll("[data-region-mode] button").forEach(item => item.classList.remove("active"));
            button.classList.add("active");
            renderRegionChart(button.dataset.mode);
        });
    });

    renderRegionChart("quantity");
    renderGoodsChart();
    renderStatusChart();
    renderTrendChart();
})();
