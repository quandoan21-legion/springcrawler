const btn = document.querySelector(".menubtn");
const menu = document.querySelector(".mobile-menu");
const closebtn = document.querySelector('.closebtn');
const body = document.body;

btn.addEventListener("click", toggle);
closebtn.addEventListener('click', toggle);

function toggle() {
    menu.classList.toggle("hidden");
    closebtn.classList.toggle("hidden");
    btn.classList.toggle("hidden");
}

