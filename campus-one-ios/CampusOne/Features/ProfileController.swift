import UIKit

/// 「我的」档案：身份卡 + 个人相关入口 + 登出。对应角色化设置入口。
final class ProfileController: UITableViewController {
    private struct Row { let title: String; let no: String; let make: (() -> UIViewController)? }
    private let rows: [Row] = [
        Row(title: "我的申请", no: "A1", make: { MyApplicationsController() }),
        Row(title: "我的报修", no: "A2", make: { MyRepairsController() }),
        Row(title: "消息中心", no: "A3", make: { MessageController() }),
        Row(title: "任务中心", no: "A4", make: { TaskController() }),
    ]

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "我的"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "cell")
        tableView.rowHeight = 62
    }

    override func numberOfSections(in tableView: UITableView) -> Int { 2 }
    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        section == 0 ? rows.count : 1
    }
    override func tableView(_ tableView: UITableView, heightForHeaderInSection section: Int) -> CGFloat { 28 }
    override func tableView(_ tableView: UITableView, viewForHeaderInSection section: Int) -> UIView? { UIView() }

    override func tableView(_ tableView: UITableView, viewForFooterInSection section: Int) -> UIView? {
        section == 0 ? idCard() : nil
    }
    override func tableView(_ tableView: UITableView, heightForFooterInSection section: Int) -> CGFloat {
        section == 0 ? 120 : 0
    }

    private func idCard() -> UIView {
        let card = ArchiveCard()
        let name = UILabel(); name.text = AuthStore.shared.realName; name.font = .systemFont(ofSize: 18, weight: .semibold); name.textColor = Theme.ink900
        let role = StampView(AuthStore.shared.roleText, style: .line)
        let sub = UILabel(); sub.text = AuthStore.shared.username; sub.font = .mono(11); sub.textColor = Theme.ink300
        let grid = UIStackView(arrangedSubviews: [name, role, UIView(), sub]); grid.spacing = 12; grid.alignment = .center
        let wrap = UIView(); wrap.backgroundColor = .clear
        card.translatesAutoresizingMaskIntoConstraints = false
        grid.translatesAutoresizingMaskIntoConstraints = false
        wrap.addSubview(card); card.addSubview(grid)
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: wrap.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: wrap.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: wrap.topAnchor, constant: 6),
            card.heightAnchor.constraint(equalToConstant: 96),
            grid.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 20),
            grid.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -20),
            grid.centerYAnchor.constraint(equalTo: card.centerYAnchor),
        ])
        return wrap
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.section == 0 {
            let r = rows[indexPath.row]
            let cell = UITableViewCell(style: .value1, reuseIdentifier: "cell")
            cell.backgroundColor = Theme.paper
            cell.textLabel?.text = r.no + "   " + r.title
            cell.textLabel?.font = .systemFont(ofSize: 15)
            cell.textLabel?.textColor = Theme.ink900
            cell.accessoryType = .disclosureIndicator
            cell.tintColor = Theme.ink300
            return cell
        }
        let cell = UITableViewCell(style: .default, reuseIdentifier: "cell")
        cell.backgroundColor = Theme.paper
        cell.textLabel?.text = "登出档案"
        cell.textLabel?.textColor = .systemRed
        cell.textLabel?.font = .systemFont(ofSize: 15, weight: .medium)
        cell.textLabel?.textAlignment = .center
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        if indexPath.section == 0, let make = rows[indexPath.row].make {
            navigationController?.pushViewController(make(), animated: true)
        } else {
            let confirm = UIAlertController(title: "登出", message: "结束本次校园档案会话？", preferredStyle: .alert)
            confirm.addAction(UIAlertAction(title: "取消", style: .cancel))
            confirm.addAction(UIAlertAction(title: "登出", style: .destructive) { _ in AppRouter.shared.switchToLogin() })
            present(confirm, animated: true)
        }
    }
}