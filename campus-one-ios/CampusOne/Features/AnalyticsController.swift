import UIKit

/// 数据中心（对应 Vue DataAnalytics）。宏观指标 + 墨阶柱状/环形图（自绘 CAShapeLayer，无第三方库）。
final class AnalyticsController: UIViewController {

    private let scrollView = UIScrollView()
    private let content = UIStackView()
    private let loading = UIActivityIndicatorView(style: .large)

    private let statsGrid = UIStackView()
    private let barChart = InkBarChartView()
    private let donutChart = InkDonutChartView()

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "数据中心"
        view.backgroundColor = Theme.paper
        buildUI()
        loadData()
    }

    private func buildUI() {
        scrollView.translatesAutoresizingMaskIntoConstraints = false
        scrollView.alwaysBounceVertical = true
        view.addSubview(scrollView)
        content.axis = .vertical
        content.spacing = Theme.space
        content.translatesAutoresizingMaskIntoConstraints = false
        scrollView.addSubview(content)
        NSLayoutConstraint.activate([
            scrollView.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor),
            scrollView.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scrollView.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scrollView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            content.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor, constant: Theme.space),
            content.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor, constant: Theme.space),
            content.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor, constant: -Theme.space),
            content.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor, constant: -Theme.space),
            content.widthAnchor.constraint(equalTo: scrollView.widthAnchor, constant: -Theme.space * 2),
        ])

        loading.translatesAutoresizingMaskIntoConstraints = false
        loading.hidesWhenStopped = true
        view.addSubview(loading)
        NSLayoutConstraint.activate([
            loading.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            loading.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])

        let header = ArchiveHeader()
        header.titleLabel.text = "数据中心"
        header.subtitleLabel.text = "校园运营数据——在案实时，墨笔为准。"
        header.markLabel.text = "DATA · 实时在案"
        content.addArrangedSubview(header)

        // 统计卡容器
        statsGrid.axis = .vertical
        statsGrid.spacing = 12
        statsGrid.isHidden = true
        content.addArrangedSubview(cardWrap(header: "总览统计", rows: statsGrid))

        // 场地运营柱状图
        let venueCard = chartCard(header: "场地预约 · 运营", chart: barChart, note: "口径：全部预约 / 今日 / 近30日日均")
        content.addArrangedSubview(venueCard)

        // 报修环形图
        let repairCard = chartCard(header: "报修工单 · 状态分布", chart: donutChart, note: "口径：已受理 / 处理中 / 已办结")
        content.addArrangedSubview(repairCard)
    }

    private func cardWrap(header: String, rows: UIStackView) -> ArchiveCard {
        let card = ArchiveCard()
        let sec = SectionHeader(header)
        sec.translatesAutoresizingMaskIntoConstraints = false
        rows.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(sec); card.addSubview(rows)
        NSLayoutConstraint.activate([
            sec.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            sec.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            sec.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            rows.topAnchor.constraint(equalTo: sec.bottomAnchor, constant: 12),
            rows.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            rows.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            rows.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -18),
        ])
        return card
    }

    private func chartCard(header: String, chart: UIView, note: String) -> ArchiveCard {
        let card = ArchiveCard()
        let sec = SectionHeader(header)
        sec.translatesAutoresizingMaskIntoConstraints = false
        let cap = UILabel(); cap.text = note; cap.font = .mono(10); cap.textColor = Theme.ink300
        card.addSubview(sec); card.addSubview(chart); card.addSubview(cap)
        chart.translatesAutoresizingMaskIntoConstraints = false
        cap.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            sec.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            sec.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            sec.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            chart.topAnchor.constraint(equalTo: sec.bottomAnchor, constant: 14),
            chart.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 12),
            chart.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -12),
            chart.heightAnchor.constraint(equalToConstant: 200),
            cap.topAnchor.constraint(equalTo: chart.bottomAnchor, constant: 8),
            cap.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            cap.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            cap.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -12),
        ])
        return card
    }

    private func loadData() {
        loading.startAnimating()
        Task {
            async let ov: AnalyticsOverview? = try? await APIClient.shared.request("GET", "analytics/overview")
            async let venue: VenueAnalytics? = try? await APIClient.shared.request("GET", "analytics/venue")
            async let repair: RepairAnalytics? = try? await APIClient.shared.request("GET", "analytics/repair")
            let (o, v, r) = await (ov, venue, repair)
            await MainActor.run {
                renderStats(o)
                self.view.layoutIfNeeded()
                if let v { barChart.render(items: [
                    ("全部预约", CGFloat(v.totalReservations ?? 0), Theme.ink900),
                    ("今日预约", CGFloat(v.todayReservations ?? 0), Theme.ink700),
                    ("近30日日均", CGFloat(v.avgDaily ?? 0), Theme.ink300),
                ]) }
                if let r { donutChart.render(items: [
                    ("已受理", CGFloat(r.accepted ?? 0), Theme.ink900),
                    ("处理中", CGFloat(r.submitted ?? 0), Theme.ink700),
                    ("已办结", CGFloat(r.resolved ?? 0), Theme.ink300),
                ]) }
                self.loading.stopAnimating()
            }
        }
    }

    private func renderStats(_ o: AnalyticsOverview?) {
        statsGrid.arrangedSubviews.forEach { $0.removeFromSuperview() }
        let stats: [(String, Int, String)] = [
            ("在案用户", o?.totalUsers ?? 0, "平台用户总数"),
            ("今日预约", o?.todayReservations ?? 0, "场地面今日排期"),
            ("待处理工单", o?.pendingRepairs ?? 0, "报修中待受理"),
            ("活动在档", o?.totalActivities ?? 0, "校园活动登记数"),
        ]
        let left = UIStackView(); left.axis = .vertical; left.spacing = 12; left.distribution = .fillEqually
        let right = UIStackView(); right.axis = .vertical; right.spacing = 12; right.distribution = .fillEqually
        stats.enumerated().forEach { i, st in
            let c = statCard(label: st.0, value: st.1, hint: st.2, no: String(format: "%02d", i + 1))
            (i % 2 == 0 ? left : right).addArrangedSubview(c)
        }
        let holder = UIStackView(arrangedSubviews: [left, right]); holder.axis = .horizontal; holder.spacing = 12; holder.distribution = .fillEqually
        statsGrid.addArrangedSubview(holder)
        statsGrid.isHidden = false
    }

    private func statCard(label: String, value: Int, hint: String, no: String) -> UIView {
        let box = ArchiveCard(); box.layer.shadowOpacity = 0
        let top = UILabel(); top.text = label; top.font = .systemFont(ofSize: 12); top.textColor = Theme.ink500
        let noL = MonoLabel(no)
        let head = UIStackView(arrangedSubviews: [top, UIView(), noL]); head.alignment = .center; head.spacing = 8
        let val = UILabel(); val.text = "\(value)"; val.font = .systemFont(ofSize: 28, weight: .semibold); val.textColor = Theme.ink900
        let hintL = UILabel(); hintL.text = hint; hintL.font = .systemFont(ofSize: 11); hintL.textColor = Theme.ink300
        [head, val, hintL].forEach { box.addSubview($0); $0.translatesAutoresizingMaskIntoConstraints = false }
        NSLayoutConstraint.activate([
            head.topAnchor.constraint(equalTo: box.topAnchor, constant: 14), head.leadingAnchor.constraint(equalTo: box.leadingAnchor, constant: 14), head.trailingAnchor.constraint(equalTo: box.trailingAnchor, constant: -14),
            val.topAnchor.constraint(equalTo: head.bottomAnchor, constant: 10), val.leadingAnchor.constraint(equalTo: box.leadingAnchor, constant: 14),
            hintL.topAnchor.constraint(equalTo: val.bottomAnchor, constant: 4), hintL.leadingAnchor.constraint(equalTo: box.leadingAnchor, constant: 14),
            hintL.bottomAnchor.constraint(equalTo: box.bottomAnchor, constant: -14),
        ])
        box.heightAnchor.constraint(greaterThanOrEqualToConstant: 110).isActive = true
        return box
    }
}

/// 墨阶柱状图（自绘 CAShapeLayer）
private final class InkBarChartView: UIView {
    func render(items: [(label: String, value: CGFloat, color: UIColor)]) {
        layer.sublayers?.forEach { $0.removeFromSuperlayer() }
        subviews.forEach { $0.removeFromSuperview() }

        let maxVal = items.map { $0.value }.max() ?? 1
        let chartH: CGFloat = 150
        let n = items.count
        let slot = bounds.width / CGFloat(n)
        let barW: CGFloat = min(44, slot * 0.5)

        for (i, item) in items.enumerated() {
            let barH = maxVal > 0 ? (item.value / maxVal) * chartH : 0
            let cx = slot * CGFloat(i) + slot / 2
            let rect = CGRect(x: cx - barW / 2, y: bounds.height - chartH - 20 + (chartH - barH), width: barW, height: barH)
            let bar = CAShapeLayer()
            let path = UIBezierPath(roundedRect: rect, cornerRadius: 3)
            bar.path = path.cgPath
            bar.fillColor = item.color.cgColor
            layer.addSublayer(bar)

            let val = UILabel()
            val.text = "\(Int(item.value))"
            val.font = .mono(10); val.textColor = Theme.ink700; val.textAlignment = .center
            val.frame = CGRect(x: rect.minX - 10, y: rect.minY - 16, width: barW + 20, height: 12)
            addSubview(val)

            let lab = UILabel()
            lab.text = item.label
            lab.font = .systemFont(ofSize: 10); lab.textColor = Theme.ink500; lab.textAlignment = .center
            lab.numberOfLines = 2
            lab.frame = CGRect(x: CGFloat(i) * slot, y: bounds.height - 20, width: slot, height: 20)
            addSubview(lab)
        }

        // 基线
        let baseline = CAShapeLayer()
        let bp = UIBezierPath()
        bp.move(to: CGPoint(x: 0, y: bounds.height - 20))
        bp.addLine(to: CGPoint(x: bounds.width, y: bounds.height - 20))
        baseline.path = bp.cgPath
        baseline.strokeColor = Theme.line.cgColor
        baseline.lineWidth = 1
        layer.addSublayer(baseline)
    }
}

/// 墨阶环形图（自绘 CAShapeLayer）
private final class InkDonutChartView: UIView {
    func render(items: [(label: String, value: CGFloat, color: UIColor)]) {
        layer.sublayers?.forEach { $0.removeFromSuperlayer() }
        subviews.forEach { $0.removeFromSuperview() }

        let filtered = items.filter { $0.value > 0 }
        let total = filtered.map { $0.value }.reduce(0, +)

        // 图例（居于环上方）
        let legend = UIStackView(); legend.axis = .horizontal; legend.spacing = 18; legend.alignment = .center
        legend.translatesAutoresizingMaskIntoConstraints = false
        addSubview(legend)
        NSLayoutConstraint.activate([
            legend.topAnchor.constraint(equalTo: topAnchor, constant: 4),
            legend.centerXAnchor.constraint(equalTo: centerXAnchor),
        ])
        for item in filtered {
            let dot = UIView(); dot.backgroundColor = item.color; dot.layer.cornerRadius = 4
            dot.translatesAutoresizingMaskIntoConstraints = false
            dot.widthAnchor.constraint(equalToConstant: 8).isActive = true
            dot.heightAnchor.constraint(equalToConstant: 8).isActive = true
            let lab = UILabel(); lab.text = "\(item.label)  \(Int(item.value))"; lab.font = .mono(11); lab.textColor = Theme.ink700
            let seg = UIStackView(arrangedSubviews: [dot, lab]); seg.spacing = 6; seg.alignment = .center
            legend.addArrangedSubview(seg)
        }

        if total <= 0 {
            let empty = UILabel(); empty.text = "暂无数据"; empty.font = .systemFont(ofSize: 13); empty.textColor = Theme.ink300
            empty.textAlignment = .center; empty.translatesAutoresizingMaskIntoConstraints = false
            addSubview(empty)
            NSLayoutConstraint.activate([
                empty.centerXAnchor.constraint(equalTo: centerXAnchor),
                empty.topAnchor.constraint(equalTo: legend.bottomAnchor, constant: 40),
            ])
            return
        }

        let center = CGPoint(x: bounds.width / 2, y: bounds.height / 2 + 28)
        let radius: CGFloat = max(44, min(bounds.width, bounds.height) * 0.26)
        let lineWidth: CGFloat = 26

        var start: CGFloat = -.pi / 2

        // 背景圆环
        let bg = CAShapeLayer()
        bg.path = UIBezierPath(arcCenter: center, radius: radius, startAngle: 0, endAngle: 2 * .pi, clockwise: true).cgPath
        bg.strokeColor = Theme.line.withAlphaComponent(0.5).cgColor
        bg.lineWidth = lineWidth
        bg.fillColor = UIColor.clear.cgColor
        layer.addSublayer(bg)

        for item in filtered {
            let frac = item.value / total
            let end = start + frac * 2 * .pi
            let seg = CAShapeLayer()
            seg.path = UIBezierPath(arcCenter: center, radius: radius, startAngle: start, endAngle: end, clockwise: true).cgPath
            seg.strokeColor = item.color.cgColor
            seg.lineWidth = lineWidth
            seg.fillColor = UIColor.clear.cgColor
            layer.addSublayer(seg)
            start = end
        }

        let sum = UILabel()
        sum.text = "\(Int(total))"
        sum.font = .systemFont(ofSize: 24, weight: .semibold); sum.textColor = Theme.ink900; sum.textAlignment = .center
        sum.frame = CGRect(x: 0, y: 0, width: 120, height: 28)
        sum.center = CGPoint(x: center.x, y: center.y - 8)
        let unit = UILabel(); unit.text = "单 · 在档"; unit.font = .mono(10); unit.textColor = Theme.ink300; unit.textAlignment = .center
        unit.frame = CGRect(x: 0, y: 0, width: 120, height: 14)
        unit.center = CGPoint(x: center.x, y: center.y + 16)
        addSubview(sum); addSubview(unit)
    }
}