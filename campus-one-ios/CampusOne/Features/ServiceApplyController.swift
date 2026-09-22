import UIKit

/// 事务申请登记表（对应 Vue ServiceApply）。请假(id=1)带类型与起止时间字段，其余仅申请事由。
final class ServiceApplyController: UIViewController {
    private let serviceId: Int
    private let reasonTextView = UITextView()

    // 请假专属
    private let leaveTypeField = InkTextField("请选择请假类型")
    private let startField = InkTextField("请选择开始时间")
    private let endField = InkTextField("请选择结束时间")

    private let submitButton = PrimaryButton("提交申请")
    private let spinner = UIActivityIndicatorView(style: .medium)

    private static let leaveTypes = ["病假", "事假", "公假", "其他"]
    private static let serviceNames: [Int: String] = [
        1: "请假申请", 2: "学生证明申请", 3: "场地特殊使用申请",
        4: "活动场地申请", 5: "物品借用申请", 6: "宿舍事务申请"
    ]

    init(serviceId: Int) {
        self.serviceId = serviceId
        super.init(nibName: nil, bundle: nil)
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) 未实现") }

    private var isLeave: Bool { serviceId == 1 }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = Theme.paper
        title = Self.serviceNames[serviceId] ?? "事务申请"
        reasonTextView.backgroundColor = .white
        reasonTextView.layer.borderWidth = 1
        reasonTextView.layer.borderColor = Theme.line.cgColor
        reasonTextView.layer.cornerRadius = 8
        reasonTextView.font = .systemFont(ofSize: 14)
        reasonTextView.textColor = Theme.ink900
        setupDateFields()
        buildUI()
    }

    private func setupDateFields() {
        for field in [startField, endField] {
            let picker = UIDatePicker()
            picker.datePickerMode = .dateAndTime
            if #available(iOS 14.0, *) { picker.preferredDatePickerStyle = .compact }
            picker.addTarget(self, action: #selector(dateChanged(_:)), for: .valueChanged)
            field.inputView = picker
        }
        // 请假类型：点击弹下拉
        leaveTypeField.addAction(UIAction { [weak self] _ in self?.pickLeaveType() }, for: .touchDown)
    }

    @objc private func dateChanged(_ sender: UIDatePicker) {
        let text = dateText(sender.date)
        if startField.inputView as? UIDatePicker === sender { startField.text = text }
        if endField.inputView as? UIDatePicker === sender { endField.text = text }
    }

    private func dateText(_ d: Date) -> String {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd HH:mm"
        return f.string(from: d)
    }

    private func buildUI() {
        let scroll = UIScrollView()
        scroll.translatesAutoresizingMaskIntoConstraints = false
        scroll.backgroundColor = .clear
        view.addSubview(scroll)
        NSLayoutConstraint.activate([
            scroll.topAnchor.constraint(equalTo: view.topAnchor),
            scroll.leadingAnchor.constraint(equalTo: view.leadingAnchor),
            scroll.trailingAnchor.constraint(equalTo: view.trailingAnchor),
            scroll.bottomAnchor.constraint(equalTo: view.bottomAnchor),
        ])

        let card = ArchiveCard()
        card.translatesAutoresizingMaskIntoConstraints = false
        scroll.addSubview(card)

        // 页眉 + 登记号
        let head = ArchiveHeader()
        head.titleLabel.text = title ?? "事务申请"
        head.subtitleLabel.text = "填写申请信息并提交"
        let mark = MonoLabel("FORM · 新立档")
        let headRow = UIStackView(arrangedSubviews: [head, mark])
        headRow.axis = .horizontal
        headRow.alignment = .center
        headRow.spacing = 8
        mark.setContentHuggingPriority(.required, for: .horizontal)

        // 请假字段区块
        var leaveStack: UIStackView?
        if isLeave {
            let typeWrap = keyValueRow("请假类型", leaveTypeField)
            let startWrap = keyValueRow("开始时间", startField)
            let endWrap = keyValueRow("结束时间", endField)
            leaveStack = UIStackView(arrangedSubviews: [typeWrap, startWrap, endWrap])
            leaveStack!.axis = .vertical
            leaveStack!.spacing = 14
        }

        let reasonLabel = fieldLabel("申请事由")
        reasonTextView.heightAnchor.constraint(equalToConstant: 130).isActive = true

        // 提交 + 动画
        submitButton.addTarget(self, action: #selector(handleSubmit), for: .touchUpInside)
        spinner.color = Theme.ink700
        spinner.hidesWhenStopped = true
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)

        var stackViews: [UIView] = [headRow]
        if let ls = leaveStack { stackViews.append(ls) }
        stackViews.append(contentsOf: [reasonLabel, reasonTextView, submitButton])

        let stack = UIStackView(arrangedSubviews: stackViews)
        stack.axis = .vertical
        stack.spacing = 14
        stack.translatesAutoresizingMaskIntoConstraints = false
        card.addSubview(stack)

        NSLayoutConstraint.activate([
            card.topAnchor.constraint(equalTo: scroll.topAnchor, constant: 20),
            card.leadingAnchor.constraint(equalTo: scroll.leadingAnchor, constant: 16),
            card.trailingAnchor.constraint(equalTo: scroll.trailingAnchor, constant: -16),
            card.bottomAnchor.constraint(equalTo: scroll.bottomAnchor, constant: -20),
            card.widthAnchor.constraint(equalTo: scroll.widthAnchor, constant: -32),

            stack.topAnchor.constraint(equalTo: card.topAnchor, constant: 20),
            stack.leadingAnchor.constraint(equalTo: card.leadingAnchor, constant: 20),
            stack.trailingAnchor.constraint(equalTo: card.trailingAnchor, constant: -20),
            stack.bottomAnchor.constraint(equalTo: card.bottomAnchor, constant: -24),

            spinner.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            spinner.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }

    private func fieldLabel(_ text: String) -> UILabel {
        let l = UILabel()
        l.text = text
        l.font = .systemFont(ofSize: 14, weight: .medium)
        l.textColor = Theme.ink900
        return l
    }

    private func keyValueRow(_ label: String, _ field: UITextField) -> UIView {
        let l = UILabel()
        l.text = label
        l.font = .systemFont(ofSize: 13)
        l.textColor = Theme.ink500
        l.heightAnchor.constraint(equalToConstant: 20).isActive = true
        l.translatesAutoresizingMaskIntoConstraints = false
        let wrap = UIView()
        wrap.addSubview(l); wrap.addSubview(field)
        l.translatesAutoresizingMaskIntoConstraints = false
        field.translatesAutoresizingMaskIntoConstraints = false
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

    private func pickLeaveType() {
        let alert = UIAlertController(title: "选择请假类型", message: nil, preferredStyle: .actionSheet)
        for t in Self.leaveTypes {
            alert.addAction(UIAlertAction(title: t, style: .default) { _ in
                self.leaveTypeField.text = t
                self.leaveTypeField.resignFirstResponder()
            })
        }
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func handleSubmit() {
        let reason = reasonTextView.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if reason.isEmpty { showAlert("请填写申请事由"); return }
        if isLeave {
            guard let type = leaveTypeField.text, !type.isEmpty,
                  !(startField.text ?? "").isEmpty, !(endField.text ?? "").isEmpty else {
                showAlert("请完整填写请假类型与起止时间"); return
            }
        }
        submitButton.isEnabled = false
        spinner.startAnimating()

        var data: [String: String] = ["reason": reason]
        if isLeave {
            data["leaveType"] = leaveTypeField.text ?? ""
            data["startTime"] = startField.text ?? ""
            data["endTime"] = endField.text ?? ""
        }
        let formData = (try? JSONSerialization.data(withJSONObject: data)).flatMap { String(data: $0, encoding: .utf8) } ?? "{}"
        let body = ApplicationPayload(serviceId: serviceId,
                                      title: Self.serviceNames[serviceId] ?? "事务申请",
                                      content: "",
                                      formData: formData)
        Task {
            do {
                try await APIClient.shared.requestVoid("POST", "applications", body: body)
                await MainActor.run {
                    self.spinner.stopAnimating()
                    let alert = UIAlertController(title: "申请已提交", message: "已登记新的申请档案。", preferredStyle: .alert)
                    alert.addAction(UIAlertAction(title: "查看我的申请", style: .default) { _ in
                        self.navigationController?.pushViewController(MyApplicationsController(), animated: true)
                    })
                    alert.addAction(UIAlertAction(title: "返回", style: .cancel) { _ in self.submitButton.isEnabled = true })
                    self.present(alert, animated: true)
                }
            } catch {
                await MainActor.run {
                    self.spinner.stopAnimating()
                    self.submitButton.isEnabled = true
                    self.showAlert(error.localizedDescription)
                }
            }
        }
    }

    private func showAlert(_ message: String) {
        let alert = UIAlertController(title: "提示", message: message, preferredStyle: .alert)
        alert.addAction(UIAlertAction(title: "知道了", style: .default))
        present(alert, animated: true)
    }
}

/// 事务申请提交载荷（/applications）
private struct ApplicationPayload: Encodable {
    let serviceId: Int
    let title: String
    let content: String
    let formData: String
}