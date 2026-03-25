from flask import Flask, request, jsonify, render_template
import pickle
import numpy as np
import os

app = Flask(__name__)

# ✅ Load trained ML model
model = pickle.load(open("accident_model.pkl", "rb"))

# ✅ HOME ROUTE
@app.route("/")
def home():
    return "Server is working 🚀"

# ✅ DRIVER PAGE (QR PAGE)
@app.route('/driver')
def driver():
    user_id = request.args.get('id')

    # TEMP DATA (for testing)
    user_data = {
        "name": "Anurag",
        "phone": "9876543210",
        "blood": "O+"
    }

    return render_template('driver.html', user=user_data, user_id=user_id)
# ✅ PREDICT ROUTE (IMPORTANT)
@app.route("/predict", methods=["POST"])
def predict():
    try:
        # ✅ Support BOTH JSON and FORM (important for Android)
        data = request.get_json() or request.form

        accX = float(data.get("accX"))
        accY = float(data.get("accY"))
        accZ = float(data.get("accZ"))
        gyroX = float(data.get("gyroX"))
        gyroY = float(data.get("gyroY"))
        gyroZ = float(data.get("gyroZ"))

        print("📥 Received:", accX, accY, accZ, gyroX, gyroY, gyroZ)

        features = np.array([[accX, accY, accZ, gyroX, gyroY, gyroZ]])

        prediction = model.predict(features)

        print("🤖 Prediction:", prediction)

        if prediction[0] == 1:
            return jsonify({"result": "accident"})
        else:
            return jsonify({"result": "Normal Driving"})

    except Exception as e:
        print("❌ ERROR:", e)
        return jsonify({"result": "Error"})

# ✅ IMPORTANT FOR RENDER
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 5000)))