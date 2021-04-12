const AWS = require("aws-sdk");

const docClient = new AWS.DynamoDB.DocumentClient({
    region: "ap-northeast-2",
});

exports.handler = async (event, context) => {
    let date = new Date().addHours(9);
    let defaultParams = { TableName: "DynamoTest" };
    let data = await docClient.scan(defaultParams).promise();
    
    let inputParams = {
        TableName: "DynamoTest",
        Item: {
            "id": data.Count,
            "Timestamp": Date.parse(date),
            "Date": date.toUTCString(),
            "Payload": `{"temperature":${event.Temp},"humidity":${event.Hum},"pressure": ${event.Press},"proximity": ${event.Proxi},"accel": ${CalculateAccel(event.Accel_X, event.Accel_Y, event.Accel_Z)}, "accel_x": ${event.Accel_X}, "accel_z": ${event.Accel_Z}}`
        }
    };

    
    await docClient.put(inputParams, function(err, data){
        console.log("start");
        if(err){
            console.log("Error!", err);
        }else{
            console.log(data);
            console.log("Good!");
        }
    }).promise();
};

Date.prototype.addHours= function(h){
    this.setHours(this.getHours()+h);
    return this;
}

function CalculateAccel(accelX, accelY, accelZ){
    return (Math.atan(accelX/accelZ) / (Math.PI/180));
}
