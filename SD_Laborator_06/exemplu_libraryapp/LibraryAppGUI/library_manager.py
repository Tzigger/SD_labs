import os
import sys
import requests
import qdarkstyle
from requests.exceptions import HTTPError
from PyQt5.QtWidgets import QWidget, QApplication, QFileDialog, QMessageBox
from PyQt5 import QtCore
from PyQt5.uic import loadUi
from urllib.parse import quote


def debug_trace(ui=None):
    from pdb import set_trace
    QtCore.pyqtRemoveInputHook()
    set_trace()
    # QtCore.pyqtRestoreInputHook()


class LibraryApp(QWidget):
    ROOT_DIR = os.path.dirname(os.path.abspath(__file__))

    def __init__(self):
        super(LibraryApp, self).__init__()
        ui_path = os.path.join(self.ROOT_DIR, 'library_manager.ui')
        loadUi(ui_path, self)
        self.search_btn.clicked.connect(self.search)
        self.save_as_file_btn.clicked.connect(self.save_as_file)

    def search(self):
        search_string = self.search_bar.text()
        selected_format = self.selected_format()
        if not search_string:
            url = '/find-and-print?format={}'.format(selected_format)
        else:
            if self.author_rb.isChecked():
                url = '/find-and-print?author={}&format={}'.format(
                    quote(search_string), selected_format)
            elif self.title_rb.isChecked():
                url = '/find-and-print?title={}&format={}'.format(
                    quote(search_string), selected_format)
            else:
                url = '/find-and-print?publisher={}&format={}'.format(
                    quote(search_string), selected_format)
        full_url = "http://localhost:8080" + url
        try:
            response = requests.get(full_url)
            self.result.setText(response.content.decode('utf-8'))
        except HTTPError as http_err:
            print('HTTP error occurred: {}'.format(http_err))
        except Exception as err:
            print('Other error occurred: {}'.format(err))

    def selected_format(self):
        if self.html_rb.isChecked():
            return 'html'
        if self.json_rb.isChecked():
            return 'json'
        return 'raw'

    def save_as_file(self):
        options = QFileDialog.Options()
        options |= QFileDialog.DontUseNativeDialog
        file_path = str(
            QFileDialog.getSaveFileName(self,
                                        'Salvare fisier',
                                        options=options))
        if file_path:
            file_path = file_path.split("'")[1]
            if not file_path.endswith('.json') and not file_path.endswith(
                    '.html') and not file_path.endswith('.txt'):
                if self.json_rb.isChecked():
                    file_path += '.json'
                elif self.html_rb.isChecked():
                    file_path += '.html'
                else:
                    file_path += '.txt'
            try:
                with open(file_path, 'w') as fp:
                    if file_path.endswith(".html"):
                        fp.write(self.result.toHtml())
                    else:
                        fp.write(self.result.toPlainText())
            except Exception as e:
                print(e)
                QMessageBox.warning(self, 'Library Manager',
                                    'Nu s-a putut salva fisierul')


if __name__ == '__main__':
    app = QApplication(sys.argv)

    stylesheet = qdarkstyle.load_stylesheet_pyqt5()
    app.setStyleSheet(stylesheet)

    window = LibraryApp()
    window.show()
    sys.exit(app.exec_())
