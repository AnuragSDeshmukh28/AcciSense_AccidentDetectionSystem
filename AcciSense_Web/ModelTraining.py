import pandas as pd
import pickle

from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import accuracy_score
from sklearn.metrics import confusion_matrix

# 1 Load dataset
data = pd.read_csv("dataset/final_accident_training_dataset.csv")

print("Dataset Loaded Successfully")
print(data.head())

# 2 Check missing values
print("\nMissing Values:")
print(data.isnull().sum())

# Remove missing values if present
data = data.dropna()

# 3 Separate features and label
X = data[['accX','accY','accZ','gyroX','gyroY','gyroZ']]
y = data['label']

# 4 Feature scaling
scaler = StandardScaler()
X_scaled = scaler.fit_transform(X)

# 5 Split dataset
X_train, X_test, y_train, y_test = train_test_split(
    X_scaled,
    y,
    test_size=0.2,
    random_state=42
)

print("\nTraining Data Size:", len(X_train))
print("Testing Data Size:", len(X_test))

# 6 Train model
model = RandomForestClassifier(n_estimators=100)

model.fit(X_train, y_train)

print("\nModel Training Completed")

# 7 Test model accuracy
predictions = model.predict(X_test)

accuracy = accuracy_score(y_test, predictions)

print("\nModel Accuracy:", accuracy)
cm = confusion_matrix(y_test, predictions)

print("\nConfusion Matrix:")
print(cm)

# 8 Save model
pickle.dump(model, open("model/accident_model.pkl", "wb"))

#print("\nModel Saved as accident_model.pkl")