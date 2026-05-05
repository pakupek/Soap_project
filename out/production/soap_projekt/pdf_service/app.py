from flask import Flask, request, jsonify, send_file
from reportlab.lib.pagesizes import A4
from reportlab.pdfgen import canvas
from reportlab.lib.units import cm
import io
import datetime

app = Flask(__name__)

@app.route('/generate-pdf', methods=['POST'])
def generate_pdf():
    data = request.get_json()

    client     = data.get('clientName', 'Nieznany')
    device     = data.get('device', '-')
    actions    = data.get('actions', '-')
    labor      = float(data.get('laborCost', 0))
    parts      = float(data.get('partsCost', 0))
    total      = labor + parts
    invoice_id = data.get('invoiceId', 'N/A')

    buffer = io.BytesIO()
    c = canvas.Canvas(buffer, pagesize=A4)
    width, height = A4

    # Nagłówek
    c.setFont("Helvetica-Bold", 22)
    c.drawString(2*cm, height - 2*cm, "FAKTURA VAT")

    c.setFont("Helvetica", 11)
    c.drawString(2*cm, height - 3*cm, f"Nr faktury: {invoice_id}")
    c.drawString(2*cm, height - 3.6*cm, f"Data: {datetime.date.today().strftime('%d.%m.%Y')}")

    # Dane klienta
    c.setFont("Helvetica-Bold", 13)
    c.drawString(2*cm, height - 5*cm, "Dane klienta:")
    c.setFont("Helvetica", 11)
    c.drawString(2*cm, height - 5.7*cm, client)

    # Urządzenie
    c.setFont("Helvetica-Bold", 13)
    c.drawString(2*cm, height - 7*cm, "Urzadzenie:")
    c.setFont("Helvetica", 11)
    c.drawString(2*cm, height - 7.7*cm, device)

    # Wykonane czynności
    c.setFont("Helvetica-Bold", 13)
    c.drawString(2*cm, height - 9*cm, "Wykonane czynnosci:")
    c.setFont("Helvetica", 11)
    y_pos = height - 9.7*cm
    for line in actions.split('\n'):
        c.drawString(2*cm, y_pos, line)
        y_pos -= 0.6*cm

    # Tabela kosztów
    y_pos -= 1*cm
    c.setFont("Helvetica-Bold", 13)
    c.drawString(2*cm, y_pos, "Zestawienie kosztow:")
    y_pos -= 0.8*cm

    c.setFont("Helvetica", 11)
    c.drawString(2*cm,   y_pos, "Robocizna:")
    c.drawString(12*cm,  y_pos, f"{labor:.2f} PLN")
    y_pos -= 0.6*cm
    c.drawString(2*cm,   y_pos, "Czesci:")
    c.drawString(12*cm,  y_pos, f"{parts:.2f} PLN")
    y_pos -= 0.3*cm
    c.line(2*cm, y_pos, 16*cm, y_pos)
    y_pos -= 0.6*cm
    c.setFont("Helvetica-Bold", 13)
    c.drawString(2*cm,  y_pos, "RAZEM:")
    c.drawString(12*cm, y_pos, f"{total:.2f} PLN")

    c.save()
    buffer.seek(0)
    return send_file(buffer, mimetype='application/pdf', as_attachment=False)

if __name__ == '__main__':
    app.run(port=5000, debug=True)