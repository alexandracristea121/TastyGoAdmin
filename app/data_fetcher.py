import firebase_admin
from firebase_admin import credentials, firestore
import pandas as pd

def fetch_data():
    # Inițializează aplicația Firebase folosind fișierul de configurare
    cred = credentials.Certificate("firebase_credentials.json")
    firebase_admin.initialize_app(cred)

    # Conectează-te la Firestore
    db = firestore.client()

    # Accesează colecția de comenzi
    orders_ref = db.collection("orders")  # Presupunem că numele colecției este "orders"

    # Extrage datele
    docs = orders_ref.stream()

    # Construiește un DataFrame din datele extrase
    data = []
    for doc in docs:
        order = doc.to_dict()
        latitude = order.get('latitude')
        longitude = order.get('longitude')
        if latitude and longitude:
            data.append([latitude, longitude])

    # Creează un DataFrame Pandas
    df = pd.DataFrame(data, columns=["latitude", "longitude"])

    return df

# Exemplu de utilizare
if __name__ == "__main__":
    data = fetch_data()
    print(data.head())  # Verifică primele rânduri
