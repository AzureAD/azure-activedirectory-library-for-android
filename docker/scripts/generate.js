var Handlebars = require('handlebars');
var fs = require('fs');
const yargs = require('yargs');

const argv = yargs
    .command('generate', 'Generates a docker file for android testing', {
        params: {
            description: 'the parameters file to merge with the template',
            alias: 'p',
            type: 'string',
        },
        outputPath: {
            description: 'where you want the generated dockerfile to be placed',
            alias: 'o',
            type: 'string'
        }
    })
    .help()
    .alias('help', 'h')
    .argv;

if (argv._.includes('generate')) {
    if(argv.params === ""){
        console.log("you didnt provide a path to the parameters file");
    }else{
        var source = fs.readFileSync('../templates/docker.handlebars', 'utf8');
        var template = Handlebars.compile(source);
        var data = fs.readFileSync(argv.params, 'utf8');
        var result = template(JSON.parse(data));
        fs.writeFileSync(argv.outputPath + '/Dockerfile', result);
    }

}
