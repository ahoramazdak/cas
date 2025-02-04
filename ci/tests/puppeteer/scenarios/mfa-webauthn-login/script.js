const puppeteer = require('puppeteer');
const assert = require('assert');
const cas = require('../../cas.js');

(async () => {
    const browser = await puppeteer.launch(cas.browserOptions());
    const page = await cas.newPage(browser);
    await cas.goto(page, "https://localhost:8443/cas/login?authn_method=mfa-webauthn");
    await cas.loginWith(page);
    await page.waitForTimeout(3000);
    await cas.screenshot(page);
    await cas.assertTextContent(page, "#status", "Login with FIDO2-enabled Device");

    let errorPanel = await page.$('#errorPanel');
    assert(await errorPanel == null);

    await cas.assertVisibility(page, '#messages');
    await cas.assertInvisibility(page, '#deviceTable');
    await cas.assertVisibility(page, '#authnButton');

    await page.on('response', response => {
        let url = response.url();
        console.log(`URL: ${url}`);
        if (url.endsWith("webauthn/authenticate")) {
            assert(response.status() === 200);
        }
    });
    await page.click("#authnButton");
    await page.waitForTimeout(1000);

    let urls = [
        "https://localhost:8443/cas/webauthn/authenticate",
        "https://localhost:8443/cas/webauthn/register"];

    await urls.forEach(url => {
        console.log(`Evaluating URL ${url}`);
        cas.doPost(url, {}, {
            'Content-Type': 'application/json'
        }, res => {
            throw(res)
        }, error => {
            assert(error.response.status === 403);
            assert(error.response.data.error === "Forbidden");
            assert(error.response.data.status === 403);
        });
    });
    
    console.log("Checking actuator endpoints...");
    const endpoints = ["health", "webAuthnDevices/casuser"];
    const baseUrl = "https://localhost:8443/cas/actuator/";
    for (let i = 0; i < endpoints.length; i++) {
        let url = baseUrl + endpoints[i];
        const response = await cas.goto(page, url);
        console.log(`${response.status()} ${response.statusText()}`);
        assert(response.ok())
    }

    await browser.close();
})();
