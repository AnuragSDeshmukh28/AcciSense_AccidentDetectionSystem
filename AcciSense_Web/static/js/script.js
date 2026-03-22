var firebaseConfig = {
    apiKey: "AIzaSyBehsxz13FrWsAsMp3YzC7uoQVE2NMjQiM",
    authDomain: "accisense.firebaseapp.com",
    databaseURL: "https://accisense-default-rtdb.firebaseio.com",
    projectId: "accisense",
    storageBucket: "accisense.firebasestorage.app",
    messagingSenderId: "275667875379",
    appId: "1:275667875379:web:99091277d5d2dad5e39546"
};

firebase.initializeApp(firebaseConfig);

const params = new URLSearchParams(window.location.search);
const driverId = params.get("id");

if(!driverId){
    document.getElementById("name").innerText = "Invalid QR Code";
    throw new Error("Driver ID missing");
}

firebase.database().ref("Users/Driver/" + driverId)
.once("value")
.then(function(snapshot){

    const data = snapshot.val();

    if(!data){
        document.getElementById("name").innerText = "Driver data not found";
        return;
    }

    document.getElementById("name").innerText = data.name || "Unknown";
    document.getElementById("phone").innerText = "Phone Number: " + (data.phone || "Not available");
    document.getElementById("vehicle").innerText = "Vehicle plate no: " + (data.vehicle || "Not available");
    document.getElementById("blood").innerText = "Blood Group: " + (data.blood || "Not available");
    document.getElementById("emergency").innerText = "Family Member Contact No.: " + (data.emergencyPhone || "Not available");

    if(data.name){
        let initials = data.name.split(" ").map(n => n[0]).join("");
        document.getElementById("profileIcon").innerText = initials;
    }

    if(data.emergencyPhone){
        document.getElementById("callBtn").href = "tel:" + data.emergencyPhone;
    }

    document.getElementById("notifyBtn").addEventListener("click", function(){
        const ownerPhone = data.phone;
        const vehicleNum = data.vehicle || "Unknown Vehicle";
        let locationLink = "Location not available";

        if(data.lat && data.lon){
            locationLink = `https://maps.google.com/?q=${data.lat},${data.lon}`;
        }

        if(ownerPhone){
            window.location.href = `sms:${ownerPhone}?body=Your vehicle (${vehicleNum}) has been reported by someone at location ${locationLink}. Please check.`;
            alert("Owner notified!");
        } else {
            alert("Owner contact not available.");
        }
    });

    document.getElementById("reportBtn").addEventListener("click", function(){

        const ownerPhone = data.phone;
        const name = data.name || "Unknown";
        const vehicleNum = data.vehicle || "Unknown Vehicle";

        let locationLink = "Location not available";

        if(data.lat && data.lon){
            locationLink = `https://maps.google.com/?q=${data.lat},${data.lon}`;
        }

        const message = `ALERT! Possible accident detected for ${name}, Vehicle: ${vehicleNum}. Location: ${locationLink}. Please respond immediately!`;

        if(ownerPhone){
            window.location.href = `sms:${ownerPhone}?body=${encodeURIComponent(message)}`;
            alert("Accident report sent to owner!");
        } else {
            alert("Owner contact not available.");
        }
    });

    if(data.lat && data.lon){
        document.getElementById("map").innerHTML = `
        <iframe
        width="100%"
        height="300"
        src="https://maps.google.com/maps?q=${data.lat},${data.lon}&hl=es;z=17&output=embed"
        frameborder="0"
        style="border:0"
        allowfullscreen>
        </iframe>
        `;
    } else {
        document.getElementById("map").innerText = "Location not available";
    }

})
.catch(function(error){
    console.error("Firebase Error:", error);
    document.getElementById("name").innerText = "Error loading data";
});