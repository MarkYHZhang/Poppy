# Poppy

## What is Poppy?
Poppy is an interactive, educational, and intelligent robot that is able to communicate with users through physical movements and vocal speeches. Our vision is to make today’s smart home systems more interactive and more lively. Instead of having your home assistant like Google Home be fixed in one place, we envision a future where your assistant is able to approach you physically and interact with you with an uniquely natural manner. Poppy will be a palm-sized robot that lives on any surfaces in your home. You will have the option to either use Google Assistant as the answering engine of Poppy. You can talk to Poppy by simply saying “Hey Poppy”, then Poppy will look around 360 degrees to determine your location and approaches you. Poppy is able to effectively walk around obstacles and terminate movement upon approach an edge on the surface. In addition, we also plan to make Poppy educational by allowing users to send movement commands to Poppy to see their code run in action (quite literally!). 

## Main Features
* Remote control through Android APP
* Programmable and also controllable through computer client
* Google Assistant Enabled
* Facial Tracking
* Self Balancing

## Major Software Components:
* Google Assistant integration with custom voice activiations
* Facial Detection/Recognition Using OpenCV and TensorFlow
* Controlling motors accordingly based on sensor inputs
* Server backend that receives user input and sends the commands to Poppy.
* User client side that is able to write and upload command to server that are to be executed on Poppy.
* A control system on the Raspberry Pi (using pi-blaster) to control the motors.

## Hardwares: 
* A Raspberry Pi
* A camera
* A microphone
* A speaker
* 2 Motors + 2 Wheels
* Dual channel full H-bridges
* Ultrasonic Proximity Sensor
* Skeletal structure

## Video DEMO (COMING SOON!)
[![Poppy Video Demonstration](https://img.youtube.com/vi/w4yrwwst9eY/0.jpg)](https://www.youtube.com/embed/w4yrwwst9eY)

## TODOS:
1. Spacial awareness using proximity sensor to detect obstacles and surface edges (Currently using OpenCV for entry level spacial awareness)
2. Integrate Amazon Alexa
3. iOS APP
4. Self Docking/Charging
