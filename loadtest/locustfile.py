import uuid
import random
from locust import HttpUser, task, between

class EWalletUser(HttpUser):
    wait_time = between(0.1, 0.5)

    def on_start(self):
        self.last_tx_id = None
        uid = uuid.uuid4().hex[:8]
        username = f"user_{uid}"
        password = "password123"
        phone = f"09{random.randint(10000000, 99999999)}"
        email = f"{username}@test.com"

        reg_res = self.client.post("/auth/register", json={
            "username": username,
            "password": password,
            "fullName": f"User {uid}",
            "phone": phone,
            "email": email
        })

        if reg_res.status_code == 201:
            login_res = self.client.post("/auth/login", json={
                "username": username,
                "password": password
            })
            if login_res.status_code == 200:
                token = login_res.json()["result"]["accessToken"]
                self.headers = {"Authorization": f"Bearer {token}"}
            else:
                self.headers = {}
        else:
            self.headers = {}

    @task(5)
    def view_profile(self):
        if self.headers:
            self.client.get("/users/me", headers=self.headers)

    @task(3)
    def view_transaction_history(self):
        if self.headers:
            page = random.choice([1, 2])
            self.client.get(f"/transactions?page={page}&size=10", headers=self.headers)

    @task(2)
    def view_transaction_detail(self):
        if self.headers and self.last_tx_id:
            self.client.get(f"/transactions/{self.last_tx_id}", headers=self.headers, name="/transactions/{id}")

    @task(1)
    def deposit_money(self):
        if self.headers:
            amount = random.choice([10000, 20000, 50000, 100000])
            res = self.client.post("/transactions/deposit", json={
                "amount": amount,
                "description": "Stress test deposit"
            }, headers=self.headers)
            if res.status_code == 200:
                body = res.json()
                if "result" in body and "id" in body["result"]:
                    self.last_tx_id = body["result"]["id"]
