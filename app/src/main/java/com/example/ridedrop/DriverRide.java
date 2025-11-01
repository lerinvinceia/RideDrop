package com.example.ridedrop;

public class DriverRide {
    private String name;
    private String phone;
    private String startPoint;
    private String destination;
    private String startTime;
    private String date;
    private String vehicleNo;
    private String carBrand;
    private String price;
    private String route;
    private String duration;
    private String passengerCount;
    private String timestamp;
    private String rideId;       // NEW: Firebase push ID for the ride
    private String driverUid;    // NEW: UID of the driver who added the ride

    // Empty constructor for Firebase
    public DriverRide() {
    }

    // Constructor without timestamp, rideId, driverUid
    public DriverRide(String name, String phone, String startPoint, String destination,
                      String startTime, String date, String vehicleNo, String carBrand,
                      String price, String route, String duration, String passengerCount) {
        this.name = name;
        this.phone = phone;
        this.startPoint = startPoint;
        this.destination = destination;
        this.startTime = startTime;
        this.date = date;
        this.vehicleNo = vehicleNo;
        this.carBrand = carBrand;
        this.price = price;
        this.route = route;
        this.duration = duration;
        this.passengerCount = passengerCount;
    }

    // New constructor: with timestamp only (no rideId, no driverUid)
    public DriverRide(String name, String phone, String startPoint, String destination,
                      String startTime, String date, String vehicleNo, String carBrand,
                      String price, String route, String duration, String passengerCount,
                      String timestamp) {
        this(name, phone, startPoint, destination, startTime, date, vehicleNo, carBrand,
                price, route, duration, passengerCount);
        this.timestamp = timestamp;
    }

    // Full constructor with timestamp, rideId, driverUid
    public DriverRide(String name, String phone, String startPoint, String destination,
                      String startTime, String date, String vehicleNo, String carBrand,
                      String price, String route, String duration, String passengerCount,
                      String timestamp, String rideId, String driverUid) {
        this(name, phone, startPoint, destination, startTime, date, vehicleNo, carBrand,
                price, route, duration, passengerCount);
        this.timestamp = timestamp;
        this.rideId = rideId;
        this.driverUid = driverUid;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStartPoint() { return startPoint; }
    public void setStartPoint(String startPoint) { this.startPoint = startPoint; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getVehicleNo() { return vehicleNo; }
    public void setVehicleNo(String vehicleNo) { this.vehicleNo = vehicleNo; }

    public String getCarBrand() { return carBrand; }
    public void setCarBrand(String carBrand) { this.carBrand = carBrand; }

    public String getPrice() { return price; }
    public void setPrice(String price) { this.price = price; }

    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getPassengerCount() { return passengerCount; }
    public void setPassengerCount(String passengerCount) { this.passengerCount = passengerCount; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getRideId() { return rideId; }
    public void setRideId(String rideId) { this.rideId = rideId; }

    public String getDriverUid() { return driverUid; }
    public void setDriverUid(String driverUid) { this.driverUid = driverUid; }

    // Validation method
    public boolean isValid() {
        return !(name == null || phone == null || startPoint == null || destination == null ||
                startTime == null || date == null || vehicleNo == null || carBrand == null ||
                price == null || route == null || duration == null || passengerCount == null ||
                name.isEmpty() || phone.isEmpty() || startPoint.isEmpty() || destination.isEmpty() ||
                startTime.isEmpty() || date.isEmpty() || vehicleNo.isEmpty() || carBrand.isEmpty() ||
                price.isEmpty() || route.isEmpty() || duration.isEmpty() || passengerCount.isEmpty());
    }
}


