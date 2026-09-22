import UIKit

/// 用户管理（对应 Vue SystemUsers）。关键词检索 + 分页，角色印章（TEACHER=教师 / COUNSELOR=职工）。
final class SystemUsersController: UITableViewController {

    private var users: [UserItem] = []
    private var total = 0
    private var page = 1
    private let pageSize = 20
    private var keyword = ""

    private let search = UISearchBar()
    private let pageLabel = MonoLabel("", size: 11)
    private var prev = UIButton(type: .system)
    private var next = UIButton(type: .system)
    private let empty = UILabel()

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "用户管理"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.rowHeight = 88
        tableView.sectionHeaderHeight = 0
        tableView.sectionFooterHeight = 0
        tableView.register(SystemUserCell.self, forCellReuseIdentifier: "cell")

        search.placeholder = "按用户名 / 姓名检索"
        search.barTintColor = Theme.paper
        search.backgroundImage = UIImage()
        search.delegate = self
        tableView.tableHeaderView = search

        let pager = makePager()
        tableView.tableFooterView = pager

        empty.text = "未检索到在案用户"
        empty.textColor = Theme.ink300
        empty.font = .systemFont(ofSize: 13)
        empty.textAlignment = .center
        empty.isHidden = true
        tableView.backgroundView = empty

        loadUsers()
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        search.sizeToFit()
        if let header = tableView.tableHeaderView {
            header.frame.size.height = 52
            tableView.tableHeaderView = header
        }
        if let footer = tableView.tableFooterView {
            footer.frame.size.height = 56
            tableView.tableFooterView = footer
        }
    }

    private func makePager() -> UIView {
        let wrap = UIView()
        pageLabel.textColor = Theme.ink500
        prev = pagerButton("← 上一页", enabled: true)
        next = pagerButton("下一页 →", enabled: true)
        prev.addTarget(self, action: #selector(goPrev), for: .touchUpInside)
        next.addTarget(self, action: #selector(goNext), for: .touchUpInside)
        let row = UIStackView(arrangedSubviews: [prev, pageLabel, next]); row.spacing = 12; row.alignment = .center
        row.translatesAutoresizingMaskIntoConstraints = false
        wrap.addSubview(row)
        NSLayoutConstraint.activate([
            row.centerXAnchor.constraint(equalTo: wrap.centerXAnchor),
            row.centerYAnchor.constraint(equalTo: wrap.centerYAnchor),
            prev.widthAnchor.constraint(equalToConstant: 96),
            next.widthAnchor.constraint(equalToConstant: 96),
        ])
        return wrap
    }

    private func pagerButton(_ title: String, enabled: Bool) -> UIButton {
        let b = UIButton(type: .system)
        b.setTitle(title, for: .normal)
        b.titleLabel?.font = .systemFont(ofSize: 13, weight: .medium)
        b.tintColor = Theme.ink700
        b.backgroundColor = Theme.surface
        b.layer.borderWidth = 1; b.layer.borderColor = Theme.line.cgColor; b.layer.cornerRadius = 6
        b.heightAnchor.constraint(equalToConstant: 34).isActive = true
        return b
    }

    @objc private func goPrev() {
        guard page > 1 else { return }
        page -= 1
        loadUsers()
    }
    @objc private func goNext() {
        guard page * pageSize < total else { return }
        page += 1
        loadUsers()
    }

    private func loadUsers() {
        Task {
            do {
                var q = ["page": "\(page)", "pageSize": "\(pageSize)"]
                if !keyword.isEmpty { q["keyword"] = keyword }
                let res: Page<UserItem> = try await APIClient.shared.request("GET", "users", query: q)
                let records = res.records ?? []
                await MainActor.run {
                    self.users = records
                    self.total = res.total ?? records.count
                    self.tableView.reloadData()
                    self.updatePager()
                    self.empty.isHidden = !self.users.isEmpty
                }
            } catch {
                await MainActor.run { self.showError(error) }
            }
        }
    }

    private func updatePager() {
        let pages = max(1, Int(ceil(Double(total) / Double(pageSize))))
        pageLabel.text = "\(page) / \(pages)"
        prev.isEnabled = page > 1
        next.isEnabled = page < pages
    }

    private func showError(_ e: Error) {
        let alert = UIAlertController(title: "载入未完成", message: e.localizedDescription, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "知道了", style: .default))
        present(alert, animated: true)
    }

    // MARK: - Table

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int { users.count }
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! SystemUserCell
        cell.bind(users[indexPath.row], index: (page - 1) * pageSize + indexPath.row + 1)
        return cell
    }
}

extension SystemUsersController: UISearchBarDelegate {
    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String) {
        keyword = searchText.trimmingCharacters(in: .whitespaces)
        page = 1
        NSObject.cancelPreviousPerformRequests(withTarget: self, selector: #selector(delayedSearch), object: nil)
        perform(#selector(delayedSearch), with: nil, afterDelay: 0.4)
    }
    @objc private func delayedSearch() { loadUsers() }
    func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
        searchBar.resignFirstResponder()
        keyword = searchBar.text?.trimmingCharacters(in: .whitespaces) ?? ""
        page = 1
        loadUsers()
    }
}

/// 用户行：编号 + 账号/姓名 + 角色章 + 状态章 + 邮箱/手机
private final class SystemUserCell: UITableViewCell {
    private let no = MonoLabel("", size: 10)
    private let username = UILabel()
    private let realName = UILabel()
    private let roleStamp = StampView("", style: .line)
    private let statusStamp = StampView("", style: .line)
    private let detail = UILabel()

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        selectionStyle = .none
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false

        realName.font = .systemFont(ofSize: 15, weight: .medium); realName.textColor = Theme.ink900
        username.font = .mono(11); username.textColor = Theme.ink300
        detail.font = .systemFont(ofSize: 11); detail.textColor = Theme.ink500
        detail.lineBreakMode = .byTruncatingMiddle
        detail.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)

        let nameBlock = UIStackView(arrangedSubviews: [realName, username]); nameBlock.axis = .vertical; nameBlock.spacing = 2; nameBlock.alignment = .leading
        realName.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)

        let topRow = UIStackView(arrangedSubviews: [no, nameBlock, UIView(), roleStamp, statusStamp])
        topRow.spacing = 10; topRow.alignment = .center

        let stack = UIStackView(arrangedSubviews: [topRow, detail]); stack.axis = .vertical; stack.spacing = 8; stack.alignment = .fill

        contentView.addSubview(card)
        card.addSubview(stack)
        stack.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 5),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -5),
            stack.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            stack.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -12),
        ])
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(_ u: UserItem, index: Int) {
        no.text = String(format: "%02d", index)
        realName.text = u.realName
        username.text = "@" + u.username
        roleStamp.text = roleText(u.role)
        roleStamp.setStyle(.line)
        statusStamp.text = u.status != 0 ? "启用" : "禁用"
        statusStamp.setStyle(u.status != 0 ? .solid : .line)
        let email = u.email ?? "—", phone = u.phone ?? "—"
        detail.text = "\(email) · \(phone)"
    }

    private func roleText(_ role: String) -> String {
        let m: [String: String] = [
            "SUPER_ADMIN": "超级管理员", "ADMIN": "管理员",
            "TEACHER": "教师", "COUNSELOR": "职工", "STUDENT": "学生",
        ]
        return m[role] ?? (role.isEmpty ? "未知" : role)
    }
}