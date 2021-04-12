
const AWS = require("aws-sdk");

const docClient = new AWS.DynamoDB.DocumentClient({
    region: "ap-northeast-2"
}); 

exports.handler = async (event) => {
    let sendData = [];
    switch(event.queryStringParameters.operator){
        case "RECENT_DATA":
            try{
                let params = { "TableName": "DynamoTest" };
                let data = await docClient.scan(params).promise();
                recentData(sendData, data);
            }catch (err){
                console.log("Error!!");
            }
            break;
        case "ALARM_DATA":
            try{
                let params = { "TableName": "DynamoTest" };
                let data = await docClient.scan(params).promise();
                alarmData(sendData, data);
            }catch (err){
                console.log("Error!!");
            }
            break;
        case "SLEEP_DATA":
            try{
                let startTime = Number(event.queryStringParameters.start);
                let endTime = Number(event.queryStringParameters.end);
                console.log(startTime);
                let params = { 
                    TableName: "DynamoTest",
                    FilterExpression: "#Timestamp >= :startTime AND #Timestamp <= :endTime",
                    ExpressionAttributeNames: { 
                        "#Timestamp": "Timestamp"
                    },
                    ExpressionAttributeValues: {
                        ":startTime": startTime,
                        ":endTime": endTime
                    }
                };
                let data = await docClient.scan(params).promise();
                sleepData(sendData, data);
            }catch (err){
                console.log("Error!!", err);
            }
        default:
            break;
    }
    
    let sendJSON = JSON.stringify(sendData);
    return { "statusCode": 200, "body": sendJSON };
};

let recentData = (sendData, data) => {
    let count = data.Count - 1;
    for(let i = 0; i <= 10; i++){
        let parseJson = JSON.parse(data.Items[count - i].Payload);
        sendData.push(parseJson);
    }
}

let sleepData = (sendData, data) => {
    let count = data.Count - 1;
    console.log(count);
    let avg = 0;
    for(let i = 0; i < (count / 100) - 1; i++){
        let avg = 0, sum = 0;
        for(let j = 100 * i; j <= 100 * (i + 1); j++){
            // console.log(j)
            let parseAccel = JSON.parse(data.Items[count - j].Payload);
            let accel = parseAccel["accel"];
            sum += accel;
        }
        avg = sum / 100;
        let obj = {
          accel: avg  
        };
        sendData.push(obj);
    }
}

let alarmData = (sendData, data) => {
    let count = data.Count - 1;
    console.log(count);
    let avg = 0;
    for(let i = 0; i < 2; i++){
        let avg = 0, sum = 0;
        for(let j = 100 * i; j <= 100 * (i + 1); j++){
            // console.log(j)
            let parseAccel = JSON.parse(data.Items[count - j].Payload);
            let accel = parseAccel["accel"];
            sum += accel;
        }
        avg = sum / 100;
        let obj = {
          accel: avg  
        };
        sendData.push(obj);
    }
}