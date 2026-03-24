from flask import Flask, request, jsonify
import pickle
import numpy as np

app = Flask(__name__)

# ✅ Load trained ML model
model = pickle.load(open("accident_model.pkl", "rb"))

# ✅ HOME ROUTE (IMPORTANT for testing)
@app.route("/")
def home():
    return "Server is working"

@app.route('/driver')
def driver():
    return render_template('driver.html')

# ✅ PREDICT ROUTE
@app.route("/predict", methods=["POST"])
@app.route("/predict", methods=["POST"])
def predict():

    try:
        accX = float(request.form.get("accX"))
        accY = float(request.form.get("accY"))
        accZ = float(request.form.get("accZ"))
        gyroX = float(request.form.get("gyroX"))
        gyroY = float(request.form.get("gyroY"))
        gyroZ = float(request.form.get("gyroZ"))

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
    
    
# ✅ RUN SERVER
if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)  