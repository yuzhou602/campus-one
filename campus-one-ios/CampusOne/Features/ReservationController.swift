import UIKit

/// 场地预约列表（对应 Vue ReservationList）。检索条件 + 场地档案卡，点击进入时间表与预约。
final class ReservationController: UITableViewController {
    private struct Resource: Decodable {
        let id: Int
        let resourceName: String?
        let buildingName: String?
        let roomNumber: String?
        let capacity: Int?
        let equipment: String?
        let needApproval: Bool?
        let availableSlots: [String]?
        // 兼容字段名
        enum CodingKeys: String, CodingKey {
            case id, capacity, needApproval, availableSlots
            case resourceName, buildingName, roomNumber, equipment
            case equipmentJson
        }
        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            id = try c.decode(Int.self, forKey: .id)
            resourceName = try c.decodeIfPresent(String.self, forKey: .resourceName)
            buildingName = try c.decodeIfPresent(String.self, forKey: .buildingName)
            roomNumber = try c.decodeIfPresent(String.self, forKey: .roomNumber)
            capacity = try c.decodeIfPresent(Int.self, forKey: .capacity)
            let eq = try c.decodeIfPresent(String.self, forKey: .equipment)
            let eqJson = try c.decodeIfPresent(String.self, forKey: .equipmentJson)
            equipment = eq ?? eqJson
            needApproval = try c.decodeIfPresent(Bool.self, forKey: .needApproval)
            availableSlots = try c.decodeIfPresent([String].self, forKey: .availableSlots)
        }
    }

    private var resources: [Resource] = []
    private let spinner = UIActivityIndicatorView(style: .medium)

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "场地预约"
        view.backgroundColor = Theme.paper
        tableView.backgroundColor = Theme.paper
        tableView.separatorStyle = .none
        tableView.register(ReservationCell.self, forCellReuseIdentifier: "cell")

        spinner.color = Theme.ink700
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        NSLayoutConstraint.activate([
            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.topAnchor.constraint(equalTo: view.safeAreaLayoutGuide.topAnchor, constant: 40),
        ])
        load()
    }

    private func load() {
        spinner.startAnimating()
        Task {
            do {
                let page: Page<Resource> = try await APIClient.shared.request("GET", "reservations/resources", query: ["page": "1", "pageSize": "20"])
                let items = page.records ?? []
                await MainActor.run {
                    self.resources = items
                    self.tableView.reloadData()
                    self.spinner.stopAnimating()
                }
            } catch {
                await MainActor.run {
                    self.spinner.stopAnimating()
                    let alert = UIAlertController(title: "读取失败", message: error.localizedDescription, preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "知道了", style: .default))
                    self.present(alert, animated: true)
                }
            }
        }
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        resources.count + 1
    }
    override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
        indexPath.row == 0 ? 96 : 226
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        if indexPath.row == 0 {
            let host = UIView(); host.backgroundColor = .clear
            let head = ArchiveHeader()
            head.titleLabel.text = "场地预约"
            head.subtitleLabel.text = "登记并预约你的校园空间。"
            head.markLabel.text = "SPACE · 在册 \(resources.count) 处"
            host.addSubview(head)
            head.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                head.leadingAnchor.constraint(equalTo: host.leadingAnchor, constant: 16),
                head.trailingAnchor.constraint(equalTo: host.trailingAnchor, constant: -16),
                head.topAnchor.constraint(equalTo: host.topAnchor, constant: 14),
            ])
            let cell = UITableViewCell(style: .default, reuseIdentifier: nil)
            cell.backgroundColor = Theme.paper; cell.selectionStyle = .none
            cell.contentView.addSubview(host)
            host.translatesAutoresizingMaskIntoConstraints = false
            NSLayoutConstraint.activate([
                host.leadingAnchor.constraint(equalTo: cell.contentView.leadingAnchor),
                host.trailingAnchor.constraint(equalTo: cell.contentView.trailingAnchor),
                host.topAnchor.constraint(equalTo: cell.contentView.topAnchor),
                host.bottomAnchor.constraint(equalTo: cell.contentView.bottomAnchor),
            ])
            return cell
        }
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath) as! ReservationCell
        cell.bind(no: String(format: "%02d", indexPath.row), item: resources[indexPath.row - 1])
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)
        guard indexPath.row >= 1 else { return }
        let item = resources[indexPath.row - 1]
        navigationController?.pushViewController(ReservationDetailController(id: item.id), animated: true)
    }
}

/// 场地卡：名称 + 位置 + 容纳 + 设备 + 可预约时段
private final class ReservationCell: UITableViewCell {
    private let noLabel = MonoLabel("", size: 10)
    private let nameLabel = UILabel()
    private let posLabel = UILabel()
    private let metaLabel = UILabel()
    private let slotsLabel = UILabel()
    private let stamp = StampView("需审批", style: .line)

    override init(style: UITableViewCell.CellStyle, reuseIdentifier: String?) {
        super.init(style: style, reuseIdentifier: reuseIdentifier)
        backgroundColor = Theme.paper
        contentView.backgroundColor = .clear
        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        contentView.addSubview(card)

        let arch = UILabel()
        arch.text = "场地档案"
        arch.font = .mono(10)
        arch.textColor = Theme.ink300

        nameLabel.font = .systemFont(ofSize: 16, weight: .semibold)
        nameLabel.textColor = Theme.ink900
        posLabel.font = .systemFont(ofSize: 12)
        posLabel.textColor = Theme.ink500
        metaLabel.font = .systemFont(ofSize: 13)
        metaLabel.textColor = Theme.ink500
        metaLabel.numberOfLines = 0
        slotsLabel.font = .mono(11)
        slotsLabel.textColor = Theme.ink700
        slotsLabel.text = "今日可预约"
        slotsLabel.font = .systemFont(ofSize: 12)

        card.addSubview(arch)
        card.addSubview(stamp)
        card.addSubview(noLabel)
        card.addSubview(nameLabel)
        card.addSubview(posLabel)
        card.addSubview(metaLabel)
        let slotFlow = UIStackView()
        slotFlow.axis = .vertical
        slotFlow.spacing = 4
        slotFlow.alignment = .leading
        slotFlow.addArrangedSubview(slotsLabel)

        let btn = PrimaryButton("查看时间表")

        [arch, stamp, noLabel, nameLabel, posLabel, metaLabel, slotFlow, btn].forEach { $0.translatesAutoresizingMaskIntoConstraints = false }
        card.addSubview(slotFlow)
        card.addSubview(btn)
        NSLayoutConstraint.activate([
            card.leadingAnchor.constraint(equalTo: contentView.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: contentView.trailingAnchor, constant: -16),
            card.topAnchor.constraint(equalTo: contentView.topAnchor, constant: 6),
            card.bottomAnchor.constraint(equalTo: contentView.bottomAnchor, constant: -6),

            arch.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            arch.topAnchor.constraint(equalTo: card.topAnchor, constant: 12),
            stamp.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            stamp.centerYAnchor.constraint(equalTo: arch.centerYAnchor),

            nameLabel.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 16),
            nameLabel.topAnchor.constraint(equalTo: arch.bottomAnchor, constant: 8),
            noLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            noLabel.centerYAnchor.constraint(equalTo: nameLabel.centerYAnchor),
            posLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
            posLabel.topAnchor.constraint(equalTo: nameLabel.bottomAnchor, constant: 3),

            metaLabel.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
            metaLabel.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            metaLabel.topAnchor.constraint(equalTo: posLabel.bottomAnchor, constant: 8),

            slotFlow.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
            slotFlow.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            slotFlow.topAnchor.constraint(equalTo: metaLabel.bottomAnchor, constant: 10),

            btn.leadingAnchor.constraint(equalTo: nameLabel.leadingAnchor),
            btn.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -16),
            btn.topAnchor.constraint(equalTo: slotFlow.bottomAnchor, constant: 10),
            btn.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -12),
        ])
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    func bind(no: String, item: ReservationController.Resource) {
        noLabel.text = "NO·\(no)"
        nameLabel.text = item.resourceName ?? "未命名场地"
        posLabel.text = [item.buildingName, item.roomNumber].compactMap { $0 }.joined(separator: " · ")
        let cap = item.capacity.map { "容纳人数：\($0)" } ?? ""
        metaLabel.text = [cap, item.equipment ?? ""].filter { !$0.isEmpty }.joined(separator: "\n")
        stamp.isHidden = !(item.needApproval ?? false)
        let slots = item.availableSlots ?? []
        if !slots.isEmpty {
            slotsLabel.text = "今日可预约：" + slots.prefix(3).joined(separator: "  ")
        }
    }
}