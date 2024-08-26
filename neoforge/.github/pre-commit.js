const fs = require("fs");

exports.preCommit = (props) => {
    const replace = (path, searchValue, replaceValue) => {
        let content = fs.readFileSync(path, "utf-8");
        if (content.match(searchValue)) {
            fs.writeFileSync(path, content.replace(searchValue, replaceValue));
            console.log(`"${path}" changed`);
        }
    };

    // replace only the version string with new version generated from config.json:
    replace("./build.gradle", /(?<=version = ")\d+\.\d+\.\d+((-\w+)+\.\d+)?(?=")/g, props.version);
};
