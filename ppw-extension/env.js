// Environment Configuration
// Toggle LOCAL_SERVER to true for local development, false for production.
const ENV = {
    LOCAL_SERVER: false,
    PROD_URL: "http://76.13.221.43:8083",
    LOCAL_URL: "http://localhost:8083"
};

ENV.BASE_URL = ENV.LOCAL_SERVER ? ENV.LOCAL_URL : ENV.PROD_URL;
