import UIKit

/// 主容器：按角色装配底部标签（对齐 Vue 侧栏的角色过滤）。
/// - 管理员/超管：首页(管理)、系统、审批(如有)、我的
/// - 教师/职工：首页、服务、审批、我的
/// - 学生：首页、服务、我的
final class MainTabController: UITabBarController {

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        let role = AuthStore.shared.role
        let isAdmin = ["ADMIN", "SUPER_ADMIN"].contains(role)
        let isApprover = ["TEACHER", "COUNSELOR"].contains(role) || isAdmin

        var tabs: [(vc: UIViewController, title: String, icon: String)] = []
        let dash = wrap(DashboardController(), "首页", "house")
        let sys = wrap(AdminHubController(), isAdmin ? "系统" : "服务", isAdmin ? "gearshape" : "square.grid.2x2")
        let mine = wrap(ProfileController(), "我的", "person")

        tabs.append((dash, "首页", "house"))
        tabs.append((sys, isAdmin ? "系统" : "服务", isAdmin ? "gearshape" : "square.grid.2x2"))
        if isApprover { tabs.append((wrap(ApprovalController(), "审批", "checkmark.seal"), "审批", "checkmark.seal")) }
        tabs.append((mine, "我的", "person"))

        viewControllers = tabs.map { $0.vc }
        tabBar.items?.enumerated().forEach { i, item in
            item.title = tabs[i].title
            item.image = UIImage(systemName: tabs[i].icon)
        }
    }

    private func wrap(_ root: UIViewController, _ title: String, _ icon: String) -> UIViewController {
        let nav = UINavigationController(rootViewController: root)
        nav.tabBarItem = UITabBarItem(title: title, image: UIImage(systemName: icon), tag: 0)
        return nav
    }
}

/// 系统/服务管理入口（管理员视角的「账户与系统管理」）。
final class AdminHubController: UITableViewController {
    private struct Entry { let title: String; let no: String; let hint: String; let make: (() -> UIViewController)?; let disabled: Bool }
    private var rows: [Entry] = []

    override func viewDidLoad() {
        super.viewDidLoad()
        title = AuthStore.shared.isAdmin ? "系统" : "服务"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(ServiceHubCell.self, forCellReuseIdentifier: "cell")
        tableView.rowHeight = 64
        tableView.tableHeaderView = header()

        if AuthStore.shared.isAdmin {
            rows = [
                Entry(title: "用户管理", no: "01", hint: "账户档案管理", make: { SystemUsersController() }, disabled: false),
                Entry(title: "审批中心", no: "02", hint: "处理在途审批", make: { ApprovalController() }, disabled: false),
                Entry(title: "数据中心", no: "03", hint: "运营宏观统计", make: { AnalyticsController() }, disabled: false),
                Entry(title: "系统管理", no: "04", hint: "平台配置（待建档）", make: { SystemController() }, disabled: true),
            ]
        } else {
            rows = [
                Entry(title: "校园事务", no: "01", hint: "发起事务申请", make: { ServiceController() }, disabled: false),
                Entry(title: "场地预约", no: "02", hint: "预约校园场地", make: { ReservationController() }, disabled: false),
                Entry(title: "校园报修", no: "03", hint: "提交报修工单", make: { RepairController() }, disabled: false),
                Entry(title: "校园活动", no: "04", hint: "活动报名", make: { ActivityController() }, disabled: false),
                Entry(title: "校园资讯", no: "05", hint: "查看通知公告", make: { NoticeController() }, disabled: false),
                Entry(title: "AI 校园助手", no: "06", hint: "智能问答", make: { AIController() }, disabled: false),
                Entry(title: "任务中心", no: "07", hint: "待办与进度", make: { TaskController() }, disabled: false),
            ]
        }
    }

    private func header() -> UIView {
        let header = ArchiveHeader()
        header.titleLabel.text = AuthStore.shared.isAdmin ? "系统管理" : "校园服务"
        header.subtitleLabel.text = AuthStore.shared.realName + " · " + AuthStore.shared.roleText
        header.markLabel.text = "ACC · " + today()
        header.translatesAutoresizingMaskIntoConstraints = false
        let wrap = UIView(frame: CGRect(x: 0, y: 0, width: 0, height: 96))
        wrap.addSubview(header)
        NSLayoutConstraint.activate([
            header.leadingAnchor.constraint(equalTo: wrap.leadingAnchor, constant: 16),
            header.trailingAnchor.constraint(equalTo: wrap.trailingAnchor, constant: -16),
            header.topAnchor.constraint(equalTo: wrap.topAnchor, constant: 14),
        ])
        return wrap
    }

    private func today() -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
        return f.string(from: Date())
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { rows.count }
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! ServiceHubCell
        cell.bindAdmin(rows[indexPath.row])
        return cell
    }
    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        let e = rows[indexPath.row]
        guard !e.disabled, let make = e.make else { return }
        navigationController?.pushViewController(make(), animated: true)
    }
}

private extension ServiceHubCell {
    func bindAdmin(_ e: AdminHubController.Entry) {
        no.text = e.no
        title.text = e.title
        hint.text = e.hint
        alpha = e.disabled ? 0.45 : 1
    }
}