import UIKit

/// 场地详情 + 预约（对应 Vue ReservationDetail）。选日期查看时段，选时段后填写预约并提交。
final class ReservationDetailController: UIViewController {
    private let id: Int
    private var resourceId: Int?

    private struct Reservation: Decodable {
        let resourceId: Int?
        let name: String?
        let location: String?
        let capacity: Int?
    }
    private struct Availability: Decodable {
        let slots: [Slot]?
    }
    private struct Slot: Decodable {
        let time: String?
        let available: Bool?
        let current: Bool?
    }
    private struct BookingBody: Encodable {
        let resourceId: Int
        let reservationDate: String
        let startTime: String
        let endTime: String
        let purpose: String
        let participantCount: Int
    }

    private var reservation: Reservation?
    private var slots: [Slot] = []
    private var selectedSlot: Slot?
    private var selectedDate = Self.today()

    private let spinner = UIActivityIndicatorView(style: .medium)

    private let dateField = InkTextField("选择日期")
    private let timeStack = UIStackView()
    private let bookingCard = ArchiveCard()
    private let purposeField = UITextView()
    private let countField = InkTextField("参与人数")
    private let confirmButton = PrimaryButton("确认预约")

    init(id: Int) {
        self.id = id
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    private static func today() -> String {
        let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"; return f.string(from: Date())
    }
    private static func dateDisplay(_ s: String) -> String { s }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        spinner.color = Theme.ink700
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        buildUI()
        load()
    }

    private func buildUI() {
        let scroll = UIScrollView()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(scroll)
        NSLayoutConstraint.activate([
            scroll.topAnchor.constraint(equalTo: view.topAnchor),
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        let head = ArchiveHeader()
        head.titleLabel.text = "场地详情"
        head.subtitleLabel.text = "选择预约时间，登记本次使用。"
        head.markLabel.text = "ROOM · 预约"

        dateField.text = selectedDate
        dateField.addAction(UIAction { [weak self] _ in self?.pickDate() }, for: .touchDown)
        let dateWrap = keyRow("选择日期", dateField)

        let slotHeader = SectionHeader("选择预约时间")
        timeStack.axis = .vertical
        timeStack.spacing = 8

        let bookingHeader = SectionHeader("预约信息")
        purposeField.backgroundColor = .white
        purposeField.layer.borderWidth = 1
        purposeField.layer.borderColor = Theme.line.cgColor
        purposeField.layer.cornerRadius = 8
        purposeField.font = .systemFont(ofSize: 14)
        purposeField.textColor = Theme.ink900
        purposeField.heightAnchor.constraint(equalToConstant: 90).isActive = true

        let countWrap = keyRow("参与人数", countField)
        countField.keyboardType = .numberPad
        confirmButton.addTarget(self, action: #selector(confirmBooking), for: .touchUpInside)

        bookingCard.layer.borderColor = Theme.ink300.cgColor
        let bStack = UIStackView(arrangedSubviews: [bookingHeader, purposeField, countWrap, confirmButton])
        bStack.axis = .vertical; bStack.spacing = 14
        bStack.translatesAutoresizingMaskIntoConstraints = false
        bookingCard.addSubview(bStack)
        bookingCard.translatesAutoresizingMaskIntoConstraints = false
        bookingCard.isHidden = true
        bookingCard.alpha = 0

        let stack = UIStackView(arrangedSubviews: [head, dateWrap, slotHeader, timeStack, bookingCard])
        stack.axis = .vertical
        stack.spacing = 18
        stack.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: scroll.leadingAnchor, constant: 16),
            stack.trailingAnchor.constraint(equalTo: scroll.trailingAnchor, constant: -16),
            stack.topAnchor.constraint(equalTo: scroll.topAnchor, constant: 20),
            stack.bottomAnchor.constraint(equalTo: scroll.bottomAnchor, constant: -20),
            stack.widthAnchor.constraint(equalTo: scroll.widthAnchor, constant: -32),
            bStack.leadingAnchor.constraint(equalTo: bookingCard.leadingAnchor, constant: 16),
            bStack.trailingAnchor.constraint(equalTo: bookingCard.trailingAnchor, constant: -16),
            bStack.topAnchor.constraint(equalTo: bookingCard.topAnchor, constant: 16),
            bStack.bottomAnchor.constraint(equalTo: bookingCard.bottomAnchor, constant: -16),

            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }

    private func keyRow(_ label: String, _ field: UITextField) -> UIView {
        let l = UILabel(); l.text = label; l.font = .systemFont(ofSize: 13); l.textColor = Theme.ink500
        l.heightAnchor.constraint(equalToConstant: 20).isActive = true
        let wrap = UIView()
        [l, field].forEach { $0.translatesAutoresizingMaskIntoConstraints = false; wrap.addSubview($0) }
        NSLayoutConstraint.activate([
            l.topAnchor.constraint(equalTo: wrap.topAnchor),
            l.leadingAnchor.constraint(equalTo: wrap.leadingAnchor),
            l.trailingAnchor.constraint(equalTo: wrap.trailingAnchor),
            field.topAnchor.constraint(equalTo: l.bottomAnchor, constant: 6),
            field.leadingAnchor.constraint(equalTo: wrap.leadingAnchor),
            field.trailingAnchor.constraint(equalTo: wrap.trailingAnchor),
            field.bottomAnchor.constraint(equalTo: wrap.bottomAnchor),
        ])
        return wrap
    }

    private func pickDate() {
        let alert = UIAlertController(title: "选择日期", message: nil, preferredStyle: .alert)
        let picker = UIDatePicker()
        picker.datePickerMode = .date
        if #available(iOS 14.0, *) { picker.preferredDatePickerStyle = .wheels }
        alert.view.addSubview(picker)
        picker.translatesAutoresizingMaskIntoConstraints = false
        NSLayoutConstraint.activate([
            picker.leadingAnchor.constraint(equalTo: alert.view.leadingAnchor, constant: 12),
            picker.trailingAnchor.constraint(equalTo: alert.view.trailingAnchor, constant: -12),
            picker.topAnchor.constraint(equalTo: alert.view.topAnchor, constant: 28),
            picker.heightAnchor.constraint(equalToConstant: 160),
        ])
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        alert.addAction(UIAlertAction(title: "确定", style: .default) { [weak self] _ in
            let f = DateFormatter(); f.dateFormat = "yyyy-MM-dd"
            self?.selectedDate = f.string(from: picker.date)
            self?.dateField.text = self?.selectedDate
            self?.load() // 重新拉取该日期时段
        })
        present(alert, animated: true)
    }

    private func load() {
        spinner.startAnimating()
        Task {
            do {
                let res: Reservation = try await APIClient.shared.request("GET", "reservations/\(id)")
                let resourceId = res.resourceId ?? id
                await MainActor.run {
                    self.reservation = res
                    // 头图信息更新（简化，用标题行展示）
                }
                // 拉取该资源、该日期的时段
                let avail: Availability = try await APIClient.shared.request(
                    "GET", "reservations/availability",
                    query: ["resourceId": String(resourceId), "date": selectedDate])
                let slots = avail.slots ?? []
                await MainActor.run {
                    self.resourceId = resourceId
                    self.slots = slots
                    self.dateField.text = self.selectedDate
                    self.renderSlots()
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

    private func renderSlots() {
        timeStack.arrangedSubviews.forEach { $0.removeFromSuperview() }
        guard !slots.isEmpty else {
            let empty = UILabel()
            empty.text = "当日无可约时段"
            empty.font = .systemFont(ofSize: 13); empty.textColor = Theme.ink300
            timeStack.addArrangedSubview(empty)
            return
        }
        for (i, slot) in slots.enumerated() {
            let row = slotRow(slot, idx: i)
            timeStack.addArrangedSubview(row)
        }
    }

    private func slotRow(_ slot: Slot, idx: Int) -> UIView {
        let row = UIButton(type: .system)
        row.backgroundColor = Theme.surface
        row.layer.borderWidth = 1
        row.layer.cornerRadius = 8
        row.contentHorizontalAlignment = .leading
        row.heightAnchor.constraint(equalToConstant: 46).isActive = true

        let isAvail = slot.available ?? false
        let dot = UIView()
        dot.backgroundColor = isAvail ? Theme.ink900 : Theme.ink300
        dot.layer.cornerRadius = 5
        dot.translatesAutoresizingMaskIntoConstraints = false
        let time = UILabel()
        time.text = slot.time?.components(separatedBy: " - ").first ?? "时段"
        time.font = .systemFont(ofSize: 14, weight: .medium)
        time.textColor = isAvail ? Theme.ink900 : Theme.ink300
        let state = UILabel()
        state.text = slot.current == true ? "当前" : (isAvail ? "可预约" : "已占用")
        state.font = .systemFont(ofSize: 12)
        state.textColor = isAvail ? Theme.ink700 : Theme.ink500
        state.textAlignment = .right
        let no = MonoLabel(String(format: "NO·%02d", idx + 1), size: 9)
        no.tint(isAvail ? Theme.ink300 : Theme.ink500)

        let stack = UIStackView(arrangedSubviews: [dot, time, no, UIView(), state])
        stack.axis = .horizontal; stack.spacing = 10; stack.alignment = .center
        stack.isUserInteractionEnabled = false
        stack.translatesAutoresizingMaskIntoConstraints = false
        row.addSubview(stack)
        NSLayoutConstraint.activate([
            stack.leadingAnchor.constraint(equalTo: row.leadingAnchor, constant: 12),
            stack.trailingAnchor.constraint(equalTo: row.trailingAnchor, constant: -12),
            stack.centerYAnchor.constraint(equalTo: row.centerYAnchor),
        ])
        row.layer.borderColor = (isAvail ? Theme.ink300 : Theme.line).cgColor
        row.isEnabled = isAvail
        if isAvail {
            row.addAction(UIAction { [weak self] _ in self?.select(slot) }, for: .touchUpInside)
        }
        return row
    }

    private func select(_ slot: Slot) {
        selectedSlot = slot
        purposeField.text = ""
        countField.text = "1"
        bookingCard.isHidden = false
        // 展开动画
        UIView.animate(withDuration: 0.22) {
            self.bookingCard.alpha = 1
        }
    }

    @objc private func confirmBooking() {
        guard let slot = selectedSlot, let rid = resourceId else { return }
        let purpose = purposeField.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if purpose.isEmpty {
            let a = UIAlertController(title: "提示", message: "请输入使用目的", preferredStyle: .alert)
            a.addAction(UIAlertAction(title: "知道了", style: .default)); present(a, animated: true); return
        }
        let count = Int(countField.text ?? "1") ?? 1
        let times = slot.time?.components(separatedBy: " - ") ?? []
        let start = times.count > 0 ? times[0] : ""
        let end = times.count > 1 ? times[1] : ""
        let body = BookingBody(resourceId: rid, reservationDate: selectedDate,
                               startTime: start, endTime: end,
                               purpose: purpose, participantCount: count)
        confirmButton.isEnabled = false
        spinner.startAnimating()
        Task {
            do {
                try await APIClient.shared.requestVoid("POST", "reservations", body: body)
                await MainActor.run {
                    self.spinner.stopAnimating(); self.confirmButton.isEnabled = true
                    let a = UIAlertController(title: "预约成功！", message: "已登记本次场地预约。", preferredStyle: .alert)
                    a.addAction(UIAlertAction(title: "知道了", style: .default) { _ in self.navigationController?.popViewController(animated: true) })
                    self.present(a, animated: true)
                }
            } catch {
                await MainActor.run {
                    self.spinner.stopAnimating(); self.confirmButton.isEnabled = true
                    let a = UIAlertController(title: "预约失败", message: error.localizedDescription, preferredStyle: .alert)
                    a.addAction(UIAlertAction(title: "知道了", style: .default)); self.present(a, animated: true)
                }
            }
        }
    }
}