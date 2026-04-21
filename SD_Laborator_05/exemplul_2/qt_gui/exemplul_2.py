import os
import sys
from urllib.parse import urlencode
from PyQt5.QtWidgets import QWidget, QApplication, QFileDialog, QMessageBox, QDialog, QFormLayout, QLineEdit, QTextEdit, QDialogButtonBox, QVBoxLayout
from PyQt5 import QtCore
from PyQt5.uic import loadUi
from mq_communication import RabbitMq


def debug_trace(ui=None):
    from pdb import set_trace
    QtCore.pyqtRemoveInputHook()
    set_trace()
    # QtCore.pyqtRestoreInputHook()


class AddBookDialog(QDialog):
    def __init__(self, parent=None):
        super(AddBookDialog, self).__init__(parent)
        self.setWindowTitle('Adauga carte')

        form_layout = QFormLayout()
        self.author_input = QLineEdit(self)
        self.title_input = QLineEdit(self)
        self.publisher_input = QLineEdit(self)
        self.text_input = QTextEdit(self)

        form_layout.addRow('Autor', self.author_input)
        form_layout.addRow('Denumire', self.title_input)
        form_layout.addRow('Editura', self.publisher_input)
        form_layout.addRow('Text', self.text_input)

        buttons = QDialogButtonBox(QDialogButtonBox.Ok | QDialogButtonBox.Cancel, self)
        buttons.accepted.connect(self.accept)
        buttons.rejected.connect(self.reject)

        layout = QVBoxLayout(self)
        layout.addLayout(form_layout)
        layout.addWidget(buttons)

    def get_book_data(self):
        return {
            'author': self.author_input.text().strip(),
            'title': self.title_input.text().strip(),
            'publisher': self.publisher_input.text().strip(),
            'text': self.text_input.toPlainText().strip()
        }


class LibraryApp(QWidget):
    ROOT_DIR = os.path.dirname(os.path.abspath(__file__))

    def __init__(self):
        super(LibraryApp, self).__init__()
        ui_path = os.path.join(self.ROOT_DIR, 'exemplul_2.ui')
        loadUi(ui_path, self)
        self.search_btn.clicked.connect(self.search)
        self.add_book_btn.clicked.connect(self.open_add_book_dialog)
        self.save_as_file_btn.clicked.connect(self.save_as_file)
        self.rabbit_mq = RabbitMq(self)
        self.last_response = ''

    def set_response(self, response):
        self.last_response = response
        if self._looks_like_html(response):
            self.result.setHtml(response)
        else:
            self.result.setPlainText(response)

    def send_request(self, request):
        if not request:
            return
        self.rabbit_mq.send_message(message=request)
        self.rabbit_mq.receive_message()

    def _looks_like_html(self, content):
        return content.lstrip().lower().startswith('<html')

    def get_selected_format(self):
        if self.json_rb.isChecked():
            return 'json'
        if self.html_rb.isChecked():
            return 'html'
        if self.xml_rb.isChecked():
            return 'xml'
        return 'text'

    def _get_search_field(self):
        if self.author_rb.isChecked():
            return 'author'
        if self.title_rb.isChecked():
            return 'title'
        return 'publisher'

    def build_request(self):
        selected_format = self.get_selected_format()
        search_string = self.search_bar.text().strip()
        if not search_string:
            return 'print:{}'.format(urlencode({'format': selected_format}))
        params = {
            'field': self._get_search_field(),
            'value': search_string,
            'format': selected_format
        }
        return 'find:{}'.format(urlencode(params))

    def search(self):
        self.send_request(self.build_request())

    def open_add_book_dialog(self):
        dialog = AddBookDialog(self)
        if dialog.exec_() != QDialog.Accepted:
            return

        data = dialog.get_book_data()
        if not all(data.values()):
            QMessageBox.warning(self, 'Exemplul 2',
                                'Toate campurile sunt obligatorii')
            return

        request = 'add:{}'.format(urlencode(data))
        self.send_request(request)
        QMessageBox.information(self, 'Exemplul 2', self.last_response)

    def save_as_file(self):
        self.send_request(self.build_request())

        options = QFileDialog.Options()
        options |= QFileDialog.DontUseNativeDialog
        file_path, _ = QFileDialog.getSaveFileName(
            self,
            'Salvare fisier',
            '',
            'JSON (*.json);;HTML (*.html);;Text (*.txt);;XML (*.xml);;All Files (*)',
            options=options)
        if file_path:
            selected_format = self.get_selected_format()
            extension = {
                'json': '.json',
                'html': '.html',
                'xml': '.xml',
                'text': '.txt'
            }[selected_format]
            if not file_path.endswith(extension):
                file_path += extension
            try:
                with open(file_path, 'w', encoding='utf-8') as fp:
                    fp.write(self.last_response)
            except Exception as e:
                print(e)
                QMessageBox.warning(self, 'Exemplul 2',
                                    'Nu s-a putut salva fisierul')


if __name__ == '__main__':
    app = QApplication(sys.argv)
    window = LibraryApp()
    window.show()
    sys.exit(app.exec_())
