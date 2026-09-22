import UIKit

/// 校园报修（对应 Vue RepairList）。报修类别（点击可快捷填报）+ 「提交报修」按钮 + 报修登记表。
final class RepairController: UIViewController {
    private struct Category { let label: String; let value: String; let symbol: String }

    private let categories: [Category] = [
        Category(label: "宿舍维修", value: "dorm", symbol: "house"),
        Category(label: "教室设备", value: "classroom", symbol: "desktopcomputer"),
        Category(label: "水电维修", value: "water", symbol: "drop"),
        Category(label: "网络故障", value: "network", symbol: "wifi"),
    ]

    private let locationField = InkTextField("报修位置，如：信息楼305")
    private let categoryField = InkTextField("请选择故障类型")
    private let descTextView = UITextView()
    private let contactField = InkTextField("联系方式")
    private let availableField = InkTextField("可维修时间，如：工作日白天")
    private let submitButton = PrimaryButton("提交")
    private let formCard = ArchiveCard()
    private let spinner = UIActivityIndicatorView(style: .medium)

    private var selectedCategory: Category?

    override func viewDidLoad() {
        super.viewDidLoad()
        title = "校园报修"
        view.backgroundColor = Theme.paper
        spinner.color = Theme.ink700
        spinner.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(spinner)
        buildUI()
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

        // 页眉 + 提交按钮
        let head = ArchiveHeader()
        head.titleLabel.text = "校园报修"
        head.subtitleLabel.text = "提交报修工单，快速解决校园设施问题。"
        head.markLabel.text = "FIX · 即时登记"

        let submitBtn = PrimaryButton("提交报修")
        submitBtn.addAction(UIAction { [weak self] _ in self?.openForm() }, for: .touchUpInside)

        // 类别网格（2 行 x 2 列）
        let catHeader = SectionHeader("报修类别")
        let grid = UIStackView()
        grid.axis = .vertical
        grid.spacing = 10
        for row in stride(from: 0, to: categories.count, by: 2) {
            let rowStack = UIStackView()
            rowStack.axis = .horizontal
            rowStack.spacing = 10
            rowStack.distribution = .fillEqually
            for j in 0..<2 {
                let idx = row + j
                if idx < categories.count {
                    rowStack.addArrangedSubview(categoryChip(categories[idx], no: String(format: "%02d", idx + 1)))
                } else {
                    rowStack.addArrangedSubview(UIView())
                }
            }
            grid.addArrangedSubview(rowStack)
        }

        // 表单（初始隐藏）
        formCard.layer.borderColor = Theme.ink300.cgColor
        descTextView.backgroundColor = .white
        descTextView.layer.borderWidth = 1
        descTextView.layer.borderColor = Theme.line.cgColor
        descTextView.layer.cornerRadius = 8
        descTextView.font = .systemFont(ofSize: 14)
        descTextView.textColor = Theme.ink900
        descTextView.heightAnchor.constraint(equalToConstant: 100).isActive = true

        categoryField.addAction(UIAction { [weak self] _ in self?.pickCategory() }, for: .touchDown)
        submitButton.addTarget(self, action: #selector(handleSubmit), for: .touchUpInside)

        let formTitle = SectionHeader("报修登记")
        let fStack = UIStackView(arrangedSubviews: [
            formTitle,
            keyRow("报修位置", locationField),
            keyRow("故障类型", categoryField),
            fieldLabel("故障描述"), descTextView,
            keyRow("联系方式", contactField),
            keyRow("可维修时间", availableField),
            submitButton,
        ])
        fStack.axis = .vertical
        fStack.spacing = 14
        fStack.translatesAutoresizingMaskIntoConstraints = false
        formCard.addSubview(fStack)
        formCard.isHidden = true
        formCard.alpha = 0

        let stack = UIStackView(arrangedSubviews: [head, submitBtn, catHeader, grid, formCard])
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
            fStack.leadingAnchor.constraint(equalTo: formCard.leadingAnchor, constant: 16),
            fStack.trailingAnchor.constraint(equalTo: formCard.trailingAnchor, constant: -16),
            fStack.topAnchor.constraint(equalTo: formCard.topAnchor, constant: 16),
            fStack.bottomAnchor.constraint(equalTo: formCard.bottomAnchor, constant: -16),
        ])
    }

    private func fieldLabel(_ text: String) -> UILabel {
        let l = UILabel(); l.text = text; l.font = .systemFont(ofSize: 14, weight: .medium); l.textColor = Theme.ink900
        return l
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

    private func categoryChip(_ cat: Category, no: String) -> UIView {
        let b = UIButton(type: .system)
        b.backgroundColor = Theme.surface
        b.layer.borderWidth = 1
        b.layer.borderColor = Theme.line.cgColor
        b.layer.cornerRadius = 8
        b.heightAnchor.constraint(equalToConstant: 84).isActive = true

        let icon = UIImageView(image: UIImage(systemName: cat.symbol))
        icon.tintColor = Theme.ink700
        icon.translatesAutoresizingMaskIntoConstraints = false
        let label = UILabel(); label.text = cat.label
        label.font = .systemFont(ofSize: 14, weight: .medium); label.textColor = Theme.ink900
        let idx = MonoLabel("NO·\(no)", size: 9)
        let v = UIStackView(arrangedSubviews: [icon, label, idx])
        v.axis = .vertical; v.alignment = .center; v.spacing = 6
        v.isUserInteractionEnabled = false
        v.translatesAutoresizingMaskIntoConstraints = false
        b.addSubview(v)
        NSLayoutConstraint.activate([
            v.centerXAnchor.constraint(equalTo: b.centerXAnchor),
            v.centerYAnchor.constraint(equalTo: b.centerYAnchor),
        ])
        b.addAction(UIAction { [weak self] _ in
            self?.selectedCategory = cat
            self?.categoryField.text = cat.label
            self?.openForm()
        }, for: .touchUpInside)
        return b
    }

    private func openForm() {
        let wasHidden = formCard.isHidden
        formCard.isHidden = false
        UIView.animate(withDuration: 0.22) {
            self.formCard.alpha = 1
        }
        _ = wasHidden
    }

    private func pickCategory() {
        let alert = UIAlertController(title: "选择故障类型", message: nil, preferredStyle: .actionSheet)
        for cat in categories {
            alert.addAction(UIAlertAction(title: cat.label, style: .default) { [weak self] _ in
                self?.selectedCategory = cat
                self?.categoryField.text = cat.label
                self?.categoryField.resignFirstResponder()
            })
        }
        alert.addAction(UIAlertAction(title: "取消", style: .cancel))
        present(alert, animated: true)
    }

    @objc private func handleSubmit() {
        let location = locationField.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let desc = descTextView.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let contact = contactField.text?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !location.isEmpty else { return showAlert("请输入报修位置") }
        guard let cat = selectedCategory else { return showAlert("请选择故障类型") }
        guard !desc.isEmpty else { return showAlert("请描述故障情况") }
        guard !contact.isEmpty else { return showAlert("请输入联系方式") }

        submitButton.isEnabled = false
        spinner.startAnimating()
        let body = RepairPayload(location: location, category: cat.value,
                                 description: desc, contact: contact,
                                 availableTime: availableField.text ?? "")
        Task {
            do {
                try await APIClient.shared.requestVoid("POST", "repairs", body: body)
                await MainActor.run {
                    self.spinner.stopAnimating(); self.submitButton.isEnabled = true
                    let a = UIAlertController(title: "报修工单已提交", message: nil, preferredStyle: .alert)
                    a.addAction(UIAlertAction(title: "查看我的报修", style: .default) { _ in
                        self.navigationController?.pushViewController(MyRepairsController(), animated: true)
                    })
                    a.addAction(UIAlertAction(title: "知道了", style: .default))
                    self.present(a, animated: true)
                }
            } catch {
                await MainActor.run {
                    self.spinner.stopAnimating(); self.submitButton.isEnabled = true
                    self.showAlert(error.localizedDescription)
                }
            }
        }
    }

    private func showAlert(_ message: String) {
        let a = UIAlertController(title: "提示", message: message, preferredStyle: .alert)
        a.addAction(UIAlertAction(title: "知道了", style: .default)); present(a, animated: true)
    }
}

/// 报修载荷（/repairs）
private struct RepairPayload: Encodable {
    let location: String
    let category: String
    let description: String
    let contact: String
    let availableTime: String
}