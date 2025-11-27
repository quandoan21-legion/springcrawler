## Login & Register Footer Contrast Update

- **Change**: Set the `.form-footer` text color to white in `src/main/resources/static/css/style.css` so the “Need an account?” and “Already have an account?” messages stand out against the dark gradient background.
- **Reason**: The previous gray text blended with the dark card, reducing readability. The new `#ffffff` value ensures sufficient contrast for accessibility.
- **Scope**: Applies automatically to both `login.html` and `register.html` because they share the `.form-footer` class.
- **Testing**: No automated tests required. Verify visually by running the app (`./mvnw spring-boot:run`) and opening `/api/v1/auth` and `/api/v1/auth/register`.
