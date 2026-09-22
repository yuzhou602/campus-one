import UIKit

/// 角色化首页（对应 Vue DashboardView）。
/// - 管理员/超管：账户与系统管理为主（运营指标 + 模块入口 + 在途审批 + 运行信息）。
/// - 教师/职工/学生：校园日常（今日课程[学生] / 待办 / 待我审批[审批角色] / 通知 / 活动）。
final class DashboardController: UIViewController {

    private let scrollView = UIScrollView()
    private let content = UIStackView()
    private let loading = UIActivityIndicatorView(style: .large)

    // 各分区的行容器
    private let statsGrid = UIStackView()
    private let moduleGrid = UIStackView()
    private let pendingStack = UIStackView()
    private let sysInfoStack = UIStackView()
    private let courseStack = UIStackView()
    private let taskStack = UIStackView()
    private let noticeStack = UIStackView()
    private let activityStack = UIStackView()

    // 空态标签
    private let pendingEmpty = emptyLabel("无在途审批，可稍作歇息")
    private let sysEmpty = emptyLabel("运行信息载入中")
    private let courseEmpty = emptyLabel("今日无课程，给自己留一点从容。")
    private let taskEmpty = emptyLabel("暂无待办，账目清爽。")
    private let noticeEmpty = emptyLabel("暂无通知")
    private let activityEmpty = emptyLabel("暂无推荐活动")

    private var isAdmin: Bool { AuthStore.shared.isAdmin }
    private var isApprover: Bool { AuthStore.shared.isApprover }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        buildBase()
        if isAdmin {
            buildAdmin()
        } else {
            buildCampus()
        }
        loadData()
    }

    // MARK: - 基础骨架

    private func buildBase() {
        title = "首页"
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

        // 加载指示器
        loading.translatesAutoresizingMaskIntoConstraints = false
        loading.hidesWhenStopped = true
        view.addSubview(loading)
        NSLayoutConstraint.activate([
            loading.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            loading.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])

        // 页眉
        let header = ArchiveHeader()
        header.titleLabel.text = gesture_word() + "，" + AuthStore.shared.realName
        header.subtitleLabel.text = "今日 \(todayCN()) · \(weekDay()) · \(AuthStore.shared.roleText)"
        header.markLabel.text = (isAdmin ? "ACCOUNT" : "REC") + " · " + todayISO()
        content.addArrangedSubview(header)
    }

    // MARK: - 管理视角

    private func buildAdmin() {
        // 运营指标
        statsGrid.axis = .vertical
        statsGrid.spacing = 12
        let statCard = wrapCard(header: "关键运营指标", rows: statsGrid, empty: nil)
        statsGrid.isHidden = true
        content.addArrangedSubview(statCard)

        // 管理模块入口
        moduleGrid.axis = .vertical
        moduleGrid.spacing = 12
        let moduleCard = wrapCard(header: "系统管理 · 进入档案", rows: moduleGrid, empty: nil)
        moduleGrid.isHidden = true
        content.addArrangedSubview(moduleCard)
        buildAdminModules()

        // 在途审批
        let pendingCard = wrapCard(header: "在途审批", rows: pendingStack, empty: pendingEmpty)
        content.addArrangedSubview(pendingCard)

        // 运行信息
        let sysCard = wrapCard(header: "运行状态", rows: sysInfoStack, empty: sysEmpty)
        content.addArrangedSubview(sysCard)
    }

    private func buildAdminModules() {
        let modules: [(title: String, icon: String, make: (() -> UIViewController)?)] = [
            ("用户管理", "person.2", { SystemUsersController() }),
            ("系统管理", "gearshape", { SystemController() }),
            ("审批中心", "checkmark.seal", { ApprovalController() }),
            ("数据中心", "chart.bar", { AnalyticsController() }),
        ]
        // 2×2 网格
        let row1 = UIStackView(); row1.axis = .horizontal; row1.spacing = 12; row1.distribution = .fillEqually
        let row2 = UIStackView(); row2.axis = .horizontal; row2.spacing = 12; row2.distribution = .fillEqually
        modules.enumerated().forEach { i, m in
            let cell = moduleButton(m.title, m.icon, no: "0\(i + 1)", make: m.make)
            (i < 2 ? row1 : row2).addArrangedSubview(cell)
        }
        moduleGrid.addArrangedSubview(row1)
        moduleGrid.addArrangedSubview(row2)
        moduleGrid.isHidden = false
    }

    // MARK: - 校园视角

    private func buildCampus() {
        // 今日课程（学生）
        if AuthStore.shared.role == "STUDENT" {
            let card = wrapCard(header: "今日课程", rows: courseStack, empty: courseEmpty)
            content.addArrangedSubview(card)
        }

        // 我的待办
        let taskCard = wrapCard(header: "我的待办", rows: taskStack, empty: taskEmpty)
        content.addArrangedSubview(taskCard)

        // 待我审批（审批角色）
        if isApprover {
            let pendingCard = wrapCard(header: "待我审批", rows: pendingStack, empty: pendingEmpty)
            content.addArrangedSubview(pendingCard)
        }

        // 校园通知
        let noticeCard = wrapCard(header: "校园通知", rows: noticeStack, empty: noticeEmpty)
        content.addArrangedSubview(noticeCard)

        // 推荐活动
        let activityCard = wrapCard(header: "推荐活动", rows: activityStack, empty: activityEmpty)
        content.addArrangedSubview(activityCard)
    }

    // MARK: - 数据加载

    private func loadData() {
        loading.startAnimating()
        let role = AuthStore.shared.role
        Task {
            if isAdmin {
                await loadAdmin()
            } else if role == "STUDENT" {
                await loadStudent()
            } else {
                await loadTeacher()
            }
            if isApprover { await loadPendingApprovals() }
            await loadNotices()
            await loadActivities()
            await MainActor.run { self.loading.stopAnimating() }
        }
    }

    private func loadAdmin() async {
        async let ov: AnalyticsOverview? = try? await APIClient.shared.request("GET", "analytics/overview")
        async let sys: SystemInfo? = try? await APIClient.shared.request("GET", "system/info")
        let (o, s) = await (ov, sys)

        let stats: [(String, Int, String)] = [
            ("在案用户", o?.totalUsers ?? 0, "平台用户总数"),
            ("今日预约", o?.todayReservations ?? 0, "场地面今日排期"),
            ("待处理工单", o?.pendingRepairs ?? 0, "报修中待受理"),
            ("活动在档", o?.totalActivities ?? 0, "校园活动登记数"),
        ]
        await MainActor.run {
            statsGrid.arrangedSubviews.forEach { $0.removeFromSuperview() }
            // 2×2：左右各一纵列
            let left = UIStackView(); left.axis = .vertical; left.spacing = 12; left.distribution = .fillEqually
            let right = UIStackView(); right.axis = .vertical; right.spacing = 12; right.distribution = .fillEqually
            stats.enumerated().forEach { i, st in
                let c = statCardView(label: st.0, value: "\(st.1)", hint: st.2, no: pad(i + 1))
                (i % 2 == 0 ? left : right).addArrangedSubview(c)
            }
            let holder = UIStackView(arrangedSubviews: [left, right]); holder.axis = .horizontal; holder.spacing = 12; holder.distribution = .fillEqually
            statsGrid.addArrangedSubview(holder)
            statsGrid.isHidden = false

            sysInfoStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
            let rows: [(String, String)] = [
                ("版本", s?.version ?? "—"), ("平台", s?.name ?? "—"),
                ("运行时", s?.javaVersion ?? "—"), ("系统", s?.osName ?? "—"),
            ]
            rows.forEach { sysInfoStack.addArrangedSubview(infoRow($0.0, value: $0.1)) }
            sysEmpty.isHidden = true
        }
    }

    private func loadStudent() async {
        guard let d: StudentDashboard = try? await APIClient.shared.request("GET", "dashboard/student") else { return }
        await MainActor.run {
            // 今日课程由 backend 的 recentActivities 字段承载（对齐 Vue getStudentDashboard）
            courseStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
            if let courses = d.recentActivities, !courses.isEmpty {
                courses.forEach { courseStack.addArrangedSubview(courseRow($0)) }
                courseEmpty.isHidden = true
            }
            fillStudentTasks(pendingRepairs: d.pendingRepairs, reservations: d.upcomingReservations)
        }
    }

    private func loadTeacher() async {
        guard let d: TeacherDashboard = try? await APIClient.shared.request("GET", "dashboard/teacher") else { return }
        await MainActor.run {
            fillStudentTasks(pendingRepairs: d.pendingRepairs, reservations: d.upcomingReservations)
        }
    }

    private func fillStudentTasks(pendingRepairs: Int?, reservations: Int?) {
        taskStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        var has = false
        if let r = pendingRepairs, r > 0 {
            taskStack.addArrangedSubview(taskRow("待处理报修", count: r)); has = true
        }
        if let u = reservations, u > 0 {
            taskStack.addArrangedSubview(taskRow("即将到来的预约", count: u)); has = true
        }
        taskEmpty.isHidden = has
    }

    private func loadPendingApprovals() async {
        guard let page: Page<ApprovalItem> = try? await APIClient.shared.request("GET", "applications/approvals/pending",
                                                                              query: ["page": "1", "pageSize": "6"]) else {
            await MainActor.run { pendingEmpty.isHidden = false }
            return
        }
        let items = page.records ?? []
        await MainActor.run {
            pendingStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
            items.forEach { pendingStack.addArrangedSubview(approvalRow($0)) }
            pendingEmpty.isHidden = !items.isEmpty
        }
    }

    private func loadNotices() async {
        guard let page: Page<NoticeItem> = try? await APIClient.shared.request("GET", "notices",
                                                                              query: ["page": "1", "pageSize": "5"]) else {
            await MainActor.run { noticeEmpty.isHidden = false }
            return
        }
        let items = page.records ?? []
        await MainActor.run {
            noticeStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
            items.forEach { noticeStack.addArrangedSubview(noticeRow($0)) }
            noticeEmpty.isHidden = !items.isEmpty
        }
    }

    private func loadActivities() async {
        guard let page: Page<ActivityItem> = try? await APIClient.shared.request("GET", "activities",
                                                                                query: ["page": "1", "pageSize": "3"]) else {
            await MainActor.run { activityEmpty.isHidden = false }
            return
        }
        let items = page.records ?? []
        await MainActor.run {
            activityStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
            items.forEach { activityStack.addArrangedSubview(activityRow($0)) }
            activityEmpty.isHidden = !items.isEmpty
        }
    }

    // MARK: - 行 / 卡片构建

    private func wrapCard(header: String, rows: UIStackView, empty: UILabel?) -> ArchiveCard {
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        let sec = SectionHeader(header)
        sec.translatesAutoresizingMaskIntoConstraints = false
        rows.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(sec)
        card.addSubview(rows)
        if let empty {
            empty.translatesAutoresizingMaskIntoConstraints = false
            card.addSubview(empty)
            NSLayoutConstraint.activate([
                empty.topAnchor.constraint(equalTo: rows.bottomAnchor, constant: 12),
                empty.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
                empty.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
                empty.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -18),
                empty.centerXAnchor.constraint(equalTo: card.centerXAnchor),
            ])
        } else {
            rows.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -18).isActive = true
        }
        NSLayoutConstraint.activate([
            sec.topAnchor.constraint(equalTo: card.topAnchor, constant: 16),
            sec.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            sec.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            rows.topAnchor.constraint(equalTo: sec.bottomAnchor, constant: 12),
            rows.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            rows.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
        ])
        return card
    }

    private static func emptyLabel(_ text: String) -> UILabel {
        let l = UILabel(); l.text = text
        l.font = .systemFont(ofSize: 13); l.textColor = Theme.ink300
        l.textAlignment = .center; l.numberOfLines = 0; l.isHidden = true
        return l
    }

    private func statCardView(label: String, value: String, hint: String, no: String) -> UIView {
        let box = ArchiveCard()
        box.layer.shadowOpacity = 0
        let top = UILabel(); top.text = label; top.font = .systemFont(ofSize: 12); top.textColor = Theme.ink500
        let noLabel = MonoLabel(no)
        let head = UIStackView(arrangedSubviews: [top, UIView(), noLabel]); head.alignment = .center
        let val = UILabel(); val.text = value; val.font = .systemFont(ofSize: 28, weight: .semibold); val.textColor = Theme.ink900
        let hintL = UILabel(); hintL.text = hint; hintL.font = .systemFont(ofSize: 11); hintL.textColor = Theme.ink300
        head.translatesAutoresizingMaskIntoConstraints = false
        val.translatesAutoresizingMaskIntoConstraints = false
        hintL.translatesAutoresizingMaskIntoConstraints = false
        [head, val, hintL].forEach { box.addSubview($0) }
        NSLayoutConstraint.activate([
            head.topAnchor.constraint(equalTo: box.topAnchor, constant: 14), head.leadingAnchor.constraint(equalTo: box.leadingAnchor, constant: 14), head.trailingAnchor.constraint(equalTo: box.trailingAnchor, constant: -14),
            val.topAnchor.constraint(equalTo: head.bottomAnchor, constant: 10), val.leadingAnchor.constraint(equalTo: box.leadingAnchor, constant: 14),
            hintL.topAnchor.constraint(equalTo: val.bottomAnchor, constant: 4), hintL.leadingAnchor.constraint(equalTo: box.leadingAnchor, constant: 14),
            hintL.bottomAnchor.constraint(equalTo: box.bottomAnchor, constant: -14),
        ])
        box.heightAnchor.constraint(greaterThanOrEqualToConstant: 110).isActive = true
        return box
    }

    private func moduleButton(_ title: String, _ icon: String, no: String, make: (() -> UIViewController)?) -> UIButton {
        let b = ButtonAction()
        var bg = UIBackgroundConfiguration.listPlainCell()
        bg.backgroundColor = Theme.surface
        bg.cornerRadius = Theme.radius
        bg.strokeColor = Theme.line
        bg.strokeWidth = 1
        b.configuration = bg
        b.onTap = { [weak self] in
            guard let vc = make?() else { return }
            self?.navigationController?.pushViewController(vc, animated: true)
        }

        let img = UIImageView(image: UIImage(systemName: icon)); img.tintColor = Theme.ink700; img.contentMode = .scaleAspectFit
        let noL = MonoLabel(no)
        let head = UIStackView(arrangedSubviews: [img, UIView(), noL]); head.spacing = 8; head.alignment = .center
        head.translatesAutoresizingMaskIntoConstraints = false
        let t = UILabel(); t.text = title; t.font = .systemFont(ofSize: 14, weight: .medium); t.textColor = Theme.ink900
        let go = UILabel(); go.text = "前往管理 →"; go.font = .systemFont(ofSize: 11); go.textColor = Theme.ink300
        let inner = UIStackView(arrangedSubviews: [head, t, go]); inner.axis = .vertical; inner.spacing = 8; inner.alignment = .leading
        inner.isUserInteractionEnabled = false; inner.translatesAutoresizingMaskIntoConstraints = false
        b.addSubview(inner)
        NSLayoutConstraint.activate([
            inner.leadingAnchor.constraint(equalTo: b.leadingAnchor, constant: 14),
            inner.trailingAnchor.constraint(equalTo: b.trailingAnchor, constant: -14),
            inner.topAnchor.constraint(equalTo: b.topAnchor, constant: 14),
            inner.bottomAnchor.constraint(equalTo: b.bottomAnchor, constant: -14),
            img.widthAnchor.constraint(equalToConstant: 22), img.heightAnchor.constraint(equalToConstant: 22),
            b.heightAnchor.constraint(equalToConstant: 118),
        ])
        return b
    }

    private func approvalRow(_ item: ApprovalItem) -> UIView {
        let no = MonoLabel(pad(0), size: 10)
        let title = UILabel(); title.text = item.applicationNo ?? "待审批申请"; title.font = .systemFont(ofSize: 13, weight: .medium); title.textColor = Theme.ink900
        let stamp = StampView(isAdmin ? "待审" : "待审", style: .line)
        title.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        let titleRow = UIStackView(arrangedSubviews: [no, title, UIView(), stamp]); titleRow.spacing = 8; titleRow.alignment = .center
        let sub = UILabel(); sub.text = (item.applicantName ?? "申请人 #\(item.applicantId ?? 0)") + " · " + (item.currentNode ?? "审批节点")
        sub.font = .systemFont(ofSize: 11); sub.textColor = Theme.ink500
        return verticalRow(children: [titleRow, sub])
    }

    private func courseRow(_ c: CVClassItem) -> UIView {
        let time = UILabel(); time.text = c.startTime ?? "--"; time.font = .systemFont(ofSize: 15, weight: .semibold); time.textColor = Theme.ink900; time.textAlignment = .right
        let end = UILabel(); end.text = c.endTime ?? ""; end.font = .systemFont(ofSize: 11); end.textColor = Theme.ink300; end.textAlignment = .right
        let timeCol = UIStackView(arrangedSubviews: [time, end]); timeCol.axis = .vertical; timeCol.spacing = 1
        timeCol.minimumContentSizeCategory = .accessibilityMedium
        let sep = UIView(); sep.backgroundColor = Theme.line; sep.widthAnchor.constraint(equalToConstant: 1).isActive = true
        sep.heightAnchor.constraint(equalToConstant: 34).isActive = true
        let name = UILabel(); name.text = c.name ?? "课程"; name.font = .systemFont(ofSize: 14, weight: .medium); name.textColor = Theme.ink900
        let loc = UILabel(); loc.text = (c.location ?? "") + (c.location?.isEmpty == false ? " · " : "") + (c.teacher ?? "")
        loc.font = .systemFont(ofSize: 12); loc.textColor = Theme.ink500
        let infoCol = UIStackView(arrangedSubviews: [name, loc]); infoCol.axis = .vertical; infoCol.spacing = 3; infoCol.alignment = .leading
        name.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        timeCol.translatesAutoresizingMaskIntoConstraints = false
        timeCol.widthAnchor.constraint(equalToConstant: 62).isActive = true
        let stamp = StampView("第\(c.week ?? 0)周", style: .line)
        let row = UIStackView(arrangedSubviews: [timeCol, sep, infoCol, UIView(), stamp]); row.spacing = 12; row.alignment = .center
        return verticalRow(children: [row])
    }

    private func taskRow(_ title: String, count: Int) -> UIView {
        let dot = UIView(); dot.backgroundColor = Theme.ink900; dot.layer.cornerRadius = 4
        dot.translatesAutoresizingMaskIntoConstraints = false
        dot.widthAnchor.constraint(equalToConstant: 8).isActive = true
        dot.heightAnchor.constraint(equalToConstant: 8).isActive = true
        let t = UILabel(); t.text = title; t.font = .systemFont(ofSize: 14, weight: .medium); t.textColor = Theme.ink900
        let c = MonoLabel("\(count) 件待处理", size: 11)
        let row = UIStackView(arrangedSubviews: [dot, t, UIView(), c]); row.spacing = 10; row.alignment = .center
        return verticalRow(children: [row])
    }

    private func noticeRow(_ n: NoticeItem) -> UIView {
        let dot = UIView(); dot.backgroundColor = (n.isRead ?? false) ? Theme.ink300.withAlphaComponent(0.5) : Theme.ink900
        dot.layer.cornerRadius = 3
        dot.translatesAutoresizingMaskIntoConstraints = false
        dot.widthAnchor.constraint(equalToConstant: 6).isActive = true
        dot.heightAnchor.constraint(equalToConstant: 6).isActive = true
        let t = UILabel(); t.text = n.title ?? ""; t.font = .systemFont(ofSize: 13, weight: .medium); t.textColor = Theme.ink900
        t.numberOfLines = 2; t.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        let time = UILabel(); time.text = n.createdAt ?? ""; time.font = .systemFont(ofSize: 11); time.textColor = Theme.ink300
        let col = UIStackView(arrangedSubviews: [t, time]); col.axis = .vertical; col.spacing = 3; col.alignment = .leading
        let row = UIStackView(arrangedSubviews: [dot, col]); row.spacing = 10; row.alignment = .top
        return verticalRow(children: [row])
    }

    private func activityRow(_ a: ActivityItem) -> UIView {
        let t = UILabel(); t.text = a.title ?? ""; t.font = .systemFont(ofSize: 14, weight: .medium); t.textColor = Theme.ink900
        let d = UILabel(); d.text = (a.startTime ?? "") + " · " + (a.location ?? ""); d.font = .systemFont(ofSize: 11); d.textColor = Theme.ink500
        let reg = a.registeredCount ?? 0
        let cap = a.capacity ?? 0
        let pct = cap > 0 ? min(1, Double(reg) / Double(cap)) : 0
        let track = UIView(); track.backgroundColor = Theme.line.withAlphaComponent(0.5); track.layer.cornerRadius = 2
        track.translatesAutoresizingMaskIntoConstraints = false
        track.heightAnchor.constraint(equalToConstant: 4).isActive = true
        let fill = UIView(); fill.backgroundColor = Theme.ink900
        fill.translatesAutoresizingMaskIntoConstraints = false
        track.addSubview(fill)
        NSLayoutConstraint.activate([
            fill.leadingAnchor.constraint(equalTo: track.leadingAnchor),
            fill.topAnchor.constraint(equalTo: track.topAnchor), fill.bottomAnchor.constraint(equalTo: track.bottomAnchor),
            fill.widthAnchor.constraint(equalTo: track.widthAnchor, multiplier: pct),
        ])
        let meta = MonoLabel("\(reg)/\(cap) 人已领档", size: 11)
        let col = UIStackView(arrangedSubviews: [t, d, track, meta]); col.axis = .vertical; col.spacing = 7; col.alignment = .leading
        return verticalRow(children: [col])
    }

    private func infoRow(_ k: String, value: String) -> UIView {
        let v = UILabel(); v.text = value; v.font = .mono(12); v.textColor = Theme.ink900
        v.lineBreakMode = .byTruncatingTail; v.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        let kLabel = UILabel(); kLabel.text = k; kLabel.font = .systemFont(ofSize: 11); kLabel.textColor = Theme.ink500
        let row = UIStackView(arrangedSubviews: [kLabel, UIView(), v]); row.spacing = 8; row.alignment = .center
        row.backgroundColor = Theme.surface
        row.layoutMargins = UIEdgeInsets(top: 10, left: 12, bottom: 10, right: 12)
        row.isLayoutMarginsRelativeArrangement = true
        row.layer.borderWidth = 1; row.layer.borderColor = Theme.line.cgColor; row.layer.cornerRadius = 6
        return verticalRow(children: [row])
    }

    /// 单行落在卡片内的通用分栏（行间 1px 墨线）
    private func verticalRow(children: [UIView]) -> UIView {
        let holder = UIStackView(arrangedSubviews: children)
        holder.axis = .vertical
        holder.spacing = 5
        holder.alignment = .fill
        let container = UIView()
        holder.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(holder)
        NSLayoutConstraint.activate([
            holder.topAnchor.constraint(equalTo: container.topAnchor, constant: 10),
            holder.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            holder.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            holder.bottomAnchor.constraint(equalTo: container.bottomAnchor, constant: -4),
        ])
        let sep = UIView(); sep.backgroundColor = Theme.line.withAlphaComponent(0.7)
        sep.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(sep)
        NSLayoutConstraint.activate([
            sep.leadingAnchor.constraint(equalTo: container.leadingAnchor),
            sep.trailingAnchor.constraint(equalTo: container.trailingAnchor),
            sep.bottomAnchor.constraint(equalTo: container.bottomAnchor),
            sep.heightAnchor.constraint(equalToConstant: 1),
        ])
        return container
    }

    // MARK: - 工具

    private func pad(_ n: Int) -> String { String(format: "%02d", n) }

    private func todayISO() -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"; return f.string(from: Date())
    }
    private func todayCN() -> String {
        let f = DateFormatter(); f.dateFormat = "M月d日"; return f.string(from: Date())
    }
    private func weekDay() -> String {
        let names = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"]
        return names[Calendar.current.component(.weekday, from: Date()) - 1]
    }
    private func gesture_word() -> String {
        let h = Calendar.current.component(.hour, from: Date())
        if h < 6 { return "凌晨好" }
        if h < 12 { return "上午好" }
        if h < 14 { return "中午好" }
        if h < 18 { return "下午好" }
        return "晚上好"
    }
}

/// 带点击动作的简单按钮容器
private final class ButtonAction: UIButton {
    var onTap: (() -> Void)?
    override init(frame: CGRect) {
        super.init(frame: frame)
        addTarget(self, action: #selector(tapped), for: .touchUpInside)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }
    @objc private func tapped() { onTap?() }
}