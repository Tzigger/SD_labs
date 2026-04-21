import random
import string
import threading
import time
from dataclasses import dataclass
from queue import Empty, Queue
from typing import Dict, List

MENU_IDS = {1, 2, 3, 4, 5}
BASE_PREP_TIME = {
    1: 0.4,
    2: 0.7,
    3: 1.1,
    4: 0.9,
    5: 1.4,
}


def _build_identifier(prefix: str) -> str:
    first = random.randint(10, 99)
    suffix = ''.join(random.choices(string.ascii_uppercase + string.digits, k=6))
    return f"{prefix}{first}{suffix}"


@dataclass
class Order:
    order_id: int
    menu_id: int
    waiter_id: str
    created_at: float


class Waiter:
    def __init__(self, waiter_id: str, kitchen_queue: Queue, delivery_queue: Queue):
        self.waiter_id = waiter_id
        self.kitchen_queue = kitchen_queue
        self.delivery_queue = delivery_queue
        self.request_queue: Queue = Queue()
        self.accepted_menu_ids = MENU_IDS
        self.placed_orders = 0
        self.served_orders = 0

    def take_order(self, order_id: int, menu_id: int):
        if menu_id not in self.accepted_menu_ids:
            raise ValueError(f"{self.waiter_id} nu poate prelua meniul {menu_id}")

        order = Order(order_id=order_id, menu_id=menu_id, waiter_id=self.waiter_id, created_at=time.time())
        self.kitchen_queue.put(order)
        self.placed_orders += 1
        print(f"[{self.waiter_id}] a preluat comanda #{order.order_id} pentru meniul {menu_id}")

    def serve_order(self, order: Order):
        service_delay = 0.15 + (order.menu_id * 0.03)
        time.sleep(service_delay)
        self.served_orders += 1
        total = time.time() - order.created_at
        print(f"[{self.waiter_id}] a servit comanda #{order.order_id} (meniu {order.menu_id}) in {total:.2f}s")


class Cook:
    def __init__(self, cook_id: str, kitchen_queue: Queue, waiter_delivery_queues: Dict[str, Queue]):
        self.cook_id = cook_id
        self.kitchen_queue = kitchen_queue
        self.waiter_delivery_queues = waiter_delivery_queues
        self.accepted_menu_ids = MENU_IDS
        self.speed_factor = random.uniform(0.8, 1.4)
        self.prepared_orders = 0

    def prepare_order(self, order: Order):
        if order.menu_id not in self.accepted_menu_ids:
            raise ValueError(f"{self.cook_id} nu poate pregati meniul {order.menu_id}")

        prep_time = BASE_PREP_TIME[order.menu_id] * self.speed_factor
        time.sleep(prep_time)
        self.prepared_orders += 1
        print(f"[{self.cook_id}] a pregatit comanda #{order.order_id} in {prep_time:.2f}s")
        self.waiter_delivery_queues[order.waiter_id].put(order)


class RestaurantSystem:
    def __init__(self, waiter_count: int, cook_count: int, total_orders: int):
        self.total_orders = total_orders
        self.kitchen_queue: Queue = Queue()
        self.done_event = threading.Event()
        self.lock = threading.Lock()
        self.completed_orders = 0

        self.waiters: List[Waiter] = []
        self.cooks: List[Cook] = []
        self.waiter_threads: List[threading.Thread] = []
        self.cook_threads: List[threading.Thread] = []

        delivery_queues = {}
        for _ in range(waiter_count):
            waiter_id = _build_identifier('Chelner')
            delivery_queue = Queue()
            delivery_queues[waiter_id] = delivery_queue
            self.waiters.append(Waiter(waiter_id, self.kitchen_queue, delivery_queue))

        for _ in range(cook_count):
            cook_id = _build_identifier('Bucatar')
            self.cooks.append(Cook(cook_id, self.kitchen_queue, delivery_queues))

    def _waiter_worker(self, waiter: Waiter):
        while not self.done_event.is_set():
            try:
                order_id, menu_id = waiter.request_queue.get(timeout=0.1)
                waiter.take_order(order_id, menu_id)
                waiter.request_queue.task_done()
            except Empty:
                pass

            try:
                finished_order = waiter.delivery_queue.get(timeout=0.1)
                waiter.serve_order(finished_order)
                waiter.delivery_queue.task_done()
                with self.lock:
                    self.completed_orders += 1
                    if self.completed_orders >= self.total_orders:
                        self.done_event.set()
            except Empty:
                pass

    def _cook_worker(self, cook: Cook):
        while not self.done_event.is_set() or not self.kitchen_queue.empty():
            try:
                order = self.kitchen_queue.get(timeout=0.1)
            except Empty:
                continue

            try:
                cook.prepare_order(order)
            finally:
                self.kitchen_queue.task_done()

    def _dispatch_customer_orders(self):
        for order_id in range(1, self.total_orders + 1):
            waiter = random.choice(self.waiters)
            menu_id = random.randint(1, 5)
            waiter.request_queue.put((order_id, menu_id))

    def start(self):
        print('Pornire simulare restaurant...')
        for waiter in self.waiters:
            thread = threading.Thread(target=self._waiter_worker, args=(waiter,), daemon=True)
            self.waiter_threads.append(thread)
            thread.start()

        for cook in self.cooks:
            thread = threading.Thread(target=self._cook_worker, args=(cook,), daemon=True)
            self.cook_threads.append(thread)
            thread.start()

        self._dispatch_customer_orders()

        self.done_event.wait()

        for waiter in self.waiters:
            waiter.request_queue.join()
            waiter.delivery_queue.join()
        self.kitchen_queue.join()

        print('\nSimulare finalizata.')
        for waiter in self.waiters:
            print(f"{waiter.waiter_id}: comenzi preluate={waiter.placed_orders}, comenzi servite={waiter.served_orders}")
        for cook in self.cooks:
            print(f"{cook.cook_id}: comenzi pregatite={cook.prepared_orders}")


if __name__ == '__main__':
    random.seed(42)
    app = RestaurantSystem(waiter_count=3, cook_count=2, total_orders=18)
    app.start()
